from __future__ import annotations

import logging
import threading
import time
from concurrent.futures import ThreadPoolExecutor

from .config import Settings
from .conflict import resolve
from .java_client import JavaAgentClient
from .llm import AnalysisChain, PeriodSummaryChain
from .memory import MemoryStore
from .periods import build_period_prompt, fallback_summary
from .rules import build_result, calculate
from .schemas import AnalysisResult, HealthAnalysisContext, TaskLease

log = logging.getLogger("healthy_agent")


class HealthAnalysisWorker:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.java = JavaAgentClient(settings)
        self.memory = MemoryStore(settings)
        self.analysis_chain = AnalysisChain(settings)
        self.period_chain = PeriodSummaryChain(settings)

    def _handle(self, lease: TaskLease) -> None:
        started = time.monotonic()
        stop, lost = threading.Event(), threading.Event()

        def renew() -> None:
            interval = max(10.0, self.settings.lease_seconds / 3)
            while not stop.wait(interval):
                try:
                    self.java.heartbeat(lease.task_id)
                except Exception:
                    log.exception("lease renewal failed task_id=%s", lease.task_id)
                    lost.set()
                    return

        renewer = threading.Thread(target=renew, name=f"lease-{lease.task_id}", daemon=True)
        renewer.start()
        try:
            context = self.java.context(lease.task_id)
            if lease.task_type == "DAILY_ANALYSIS":
                self._process_daily(lease, context, started, lost)
            elif lease.task_type in {"BIWEEKLY_SUMMARY", "MONTHLY_SUMMARY"}:
                self._process_period(lease, context, started, lost)
            else:
                raise ValueError(f"unsupported task type: {lease.task_type}")
        except Exception as exc:
            log.exception("failed task_id=%s", lease.task_id)
            try:
                self.java.fail(lease, "AGENT_PROCESSING_ERROR", str(exc), True)
            except Exception:
                log.exception("could not report failure task_id=%s", lease.task_id)
        finally:
            stop.set()
            renewer.join(timeout=2)

    def _ensure_alive(self, started: float, lost: threading.Event) -> None:
        if lost.is_set() or time.monotonic() - started >= self.settings.max_task_seconds:
            raise TimeoutError("task exceeded five-minute execution limit or lost its lease")

    def _process_daily(self, lease: TaskLease, context: HealthAnalysisContext, started: float, lost: threading.Event) -> None:
        summary = calculate(context)
        score, risk, suggestion, recommendations = build_result(context, summary)
        fallback = AnalysisResult(healthScore=score, riskSummary=risk, optimizationSuggestion=suggestion,
                                  recommendations=[item.model_dump(by_alias=True) for item in recommendations])
        memories = self.memory.recall(context.user_id, risk, "long") + self.memory.recall(context.user_id, risk, "short")
        candidate = self.analysis_chain.invoke(context, str(summary), str(memories))
        result = resolve(candidate, context, fallback) if candidate else fallback
        self._ensure_alive(started, lost)
        completion = self.java.complete(lease, result)
        if completion["status"] == "SUCCEEDED":
            self.memory.remember(context, summary, result.optimization_suggestion, "short")
            self.memory.remember(context, summary, result.risk_summary, "long")

    def _process_period(self, lease: TaskLease, context: HealthAnalysisContext, started: float, lost: threading.Event) -> None:
        period_type = "BIWEEKLY" if lease.task_type == "BIWEEKLY_SUMMARY" else "MONTHLY"
        summary = self.period_chain.invoke(period_type, build_period_prompt(context, period_type)) or fallback_summary(context, period_type)
        summary = summary.model_copy(update={"period_type": period_type, "period_start": lease.period_start or summary.period_start, "period_end": lease.period_end or summary.period_end})
        self._ensure_alive(started, lost)
        completion = self.java.complete_period(lease, summary, lease.source_revision)
        if completion["status"] == "SUCCEEDED":
            self.memory.remember_period(context.user_id, summary, lease.model_version)

    def run_forever(self) -> None:
        logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
        try:
            with ThreadPoolExecutor(max_workers=self.settings.concurrency, thread_name_prefix="analysis") as pool:
                futures = set()
                while True:
                    futures = {future for future in futures if not future.done()}
                    while len(futures) < self.settings.concurrency:
                        lease = self.java.claim()
                        if not lease:
                            break
                        futures.add(pool.submit(self._handle, lease))
                    if not futures:
                        time.sleep(self.settings.poll_seconds)
                    else:
                        time.sleep(0.1)
        finally:
            self.java.close()
