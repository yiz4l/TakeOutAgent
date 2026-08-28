from __future__ import annotations
import logging, time
from .config import Settings
from .java_client import JavaAgentClient
from .memory import MemoryStore
from .rules import build_result, calculate
from .schemas import AnalysisResult
from .llm import AnalysisChain
from .conflict import resolve
log = logging.getLogger("healthy_agent")

class HealthAnalysisWorker:
    def __init__(self, settings): self.settings, self.java, self.memory, self.chain = settings, JavaAgentClient(settings), MemoryStore(settings), AnalysisChain(settings)
    def run_once(self):
        lease = self.java.claim()
        if not lease: return False
        try:
            context = self.java.context(lease.task_id)
            summary = calculate(context)
            score, risk, suggestion, recs = build_result(context, summary)
            fallback = AnalysisResult(healthScore=score, riskSummary=risk, optimizationSuggestion=suggestion, recommendations=[r.model_dump(by_alias=True) for r in recs])
            memories = self.memory.recall(context.user_id, risk, "long") + self.memory.recall(context.user_id, risk, "short")
            candidate = self.chain.invoke(context, str(summary), str(memories))
            result = resolve(candidate, context, fallback) if candidate else fallback
            self.memory.remember(context, summary, result.optimization_suggestion, "short")
            self.memory.remember(context, summary, result.risk_summary, "long")
            self.java.complete(lease, result); log.info("completed task_id=%s", lease.task_id)
        except Exception as exc:
            log.exception("failed task_id=%s", lease.task_id)
            try: self.java.fail(lease, "AGENT_PROCESSING_ERROR", str(exc), True)
            except Exception: log.exception("could not report failure task_id=%s", lease.task_id)
        return True
    def run_forever(self):
        logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
        try:
            while True:
                if not self.run_once(): time.sleep(self.settings.poll_seconds)
        finally: self.java.close()
