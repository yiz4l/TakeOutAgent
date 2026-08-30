from __future__ import annotations

import logging
import time

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

    def run_once(self) -> bool:
        lease = self.java.claim()
        if not lease:
            return False
        try:
            context = self.java.context(lease.task_id)
            if lease.task_type == "DAILY_ANALYSIS":
                self._process_daily(lease, context)
            elif lease.task_type in {"BIWEEKLY_SUMMARY", "MONTHLY_SUMMARY"}:
                self._process_period(lease, context)
            else:
                raise ValueError(f"unsupported task type: {lease.task_type}")
            log.info("completed task_id=%s type=%s", lease.task_id, lease.task_type)
        except Exception as exc:
            log.exception("failed task_id=%s", lease.task_id)
            try:
                self.java.fail(lease, "AGENT_PROCESSING_ERROR", str(exc), True)
            except Exception:
                log.exception("could not report failure task_id=%s", lease.task_id)
        return True

    def _process_daily(self, lease: TaskLease, context: HealthAnalysisContext) -> None:
        summary = calculate(context)
        score, risk, suggestion, recommendations = build_result(context, summary)
        fallback = AnalysisResult(
            healthScore=score,
            riskSummary=risk,
            optimizationSuggestion=suggestion,
            recommendations=[item.model_dump(by_alias=True) for item in recommendations],
        )
        memories = self.memory.recall(context.user_id, risk, "long")
        memories += self.memory.recall(context.user_id, risk, "short")
        candidate = self.analysis_chain.invoke(context, str(summary), str(memories))
        result = resolve(candidate, context, fallback) if candidate else fallback
        completion = self.java.complete(lease, result)
        if completion["status"] != "SUCCEEDED":
            log.info("task not published task_id=%s status=%s", lease.task_id, completion["status"])
            return
        # MySQL completion is authoritative; cache memories only after it succeeds.
        self.memory.remember(context, summary, result.optimization_suggestion, "short")
        self.memory.remember(context, summary, result.risk_summary, "long")

    def _process_period(self, lease: TaskLease, context: HealthAnalysisContext) -> None:
        period_type = "BIWEEKLY" if lease.task_type == "BIWEEKLY_SUMMARY" else "MONTHLY"
        facts = build_period_prompt(context, period_type)
        summary = self.period_chain.invoke(period_type, facts)
        if summary is None:
            summary = fallback_summary(context, period_type)
        period_start = lease.period_start or summary.period_start
        period_end = lease.period_end or summary.period_end
        summary = summary.model_copy(update={
            "period_type": period_type,
            "period_start": period_start,
            "period_end": period_end,
        })
        completion = self.java.complete_period(lease, summary, lease.source_revision)
        if completion["status"] != "SUCCEEDED":
            log.info("period summary not published task_id=%s status=%s", lease.task_id, completion["status"])
            return
        # Write the semantic copy only after the MySQL transaction succeeds.
        self.memory.remember_period(context.user_id, summary, lease.model_version)

    def run_forever(self) -> None:
        logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
        try:
            while True:
                if not self.run_once():
                    time.sleep(self.settings.poll_seconds)
        finally:
            self.java.close()
