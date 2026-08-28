from __future__ import annotations
import httpx
from .config import Settings
from .schemas import AnalysisResult, HealthAnalysisContext, TaskLease

class JavaAgentClient:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.client = httpx.Client(base_url=settings.java_base_url, headers={"X-Agent-Service-Token": settings.agent_service_token}, timeout=settings.request_timeout)
    def close(self): self.client.close()
    def claim(self):
        r = self.client.post("/internal/agent/health-analysis-tasks/claim", json={"workerId": self.settings.worker_id, "leaseSeconds": self.settings.lease_seconds}); r.raise_for_status(); data = r.json().get("data"); return TaskLease.model_validate(data) if data else None
    def context(self, task_id):
        r = self.client.get(f"/internal/agent/health-analysis-tasks/{task_id}/context", headers={"X-Agent-Worker-Id": self.settings.worker_id}); r.raise_for_status(); return HealthAnalysisContext.model_validate(r.json()["data"])
    def heartbeat(self, task_id):
        r = self.client.post(f"/internal/agent/health-analysis-tasks/{task_id}/heartbeat", json={"workerId": self.settings.worker_id, "leaseSeconds": self.settings.lease_seconds}); r.raise_for_status()
    def complete(self, lease: TaskLease, result: AnalysisResult):
        body = {"workerId": self.settings.worker_id, "inputRevision": lease.input_revision, "modelVersion": lease.model_version, **result.model_dump(by_alias=True)}
        r = self.client.post(f"/internal/agent/health-analysis-tasks/{lease.task_id}/complete", headers={"Idempotency-Key": lease.request_id}, json=body); r.raise_for_status()
    def fail(self, lease, code, message, retryable=True):
        r = self.client.post(f"/internal/agent/health-analysis-tasks/{lease.task_id}/fail", json={"workerId": self.settings.worker_id, "retryable": retryable, "errorCode": code, "errorMessage": message[:500]}); r.raise_for_status()

