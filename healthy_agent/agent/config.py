from __future__ import annotations
import os
import socket
import uuid
from dataclasses import dataclass
from dotenv import load_dotenv


def env_bool(name: str, default: bool = False) -> bool:
    """Parse boolean environment variables without treating 'false' as truthy."""
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}

@dataclass(frozen=True)
class Settings:
    java_base_url: str
    agent_service_token: str
    worker_id: str
    lease_seconds: int = 120
    poll_seconds: float = 3.0
    request_timeout: float = 20.0
    qdrant_url: str = "http://localhost:6333"
    qdrant_api_key: str | None = None
    short_memory_collection: str = "agent_short_memory"
    long_memory_collection: str = "agent_long_memory"
    embedding_dim: int = 1536
    enable_qdrant: bool = False
    enable_llm: bool = False
    openai_chat_model: str = "gpt-4o-mini"
    openai_embedding_model: str = "text-embedding-3-small"
    openai_temperature: float = 0

    @classmethod
    def from_env(cls) -> "Settings":
        load_dotenv()
        worker_id = os.getenv("AGENT_WORKER_ID") or (
            f"diet-agent-{socket.gethostname()}-{uuid.uuid4().hex[:8]}"
        )
        return cls(
            java_base_url=os.getenv("JAVA_AGENT_BASE_URL", "http://localhost:8080").rstrip("/"),
            agent_service_token=os.getenv("AGENT_SERVICE_TOKEN", ""),
            worker_id=worker_id,
            lease_seconds=max(30, min(600, int(os.getenv("AGENT_LEASE_SECONDS", "120")))),
            poll_seconds=float(os.getenv("AGENT_POLL_SECONDS", "3")),
            request_timeout=float(os.getenv("AGENT_REQUEST_TIMEOUT", "20")),
            qdrant_url=os.getenv("QDRANT_URL", "http://localhost:6333"),
            qdrant_api_key=os.getenv("QDRANT_API_KEY") or None,
            short_memory_collection=os.getenv(
                "QDRANT_COLLECTION_SHORT_MEMORY", "agent_short_memory"
            ),
            long_memory_collection=os.getenv(
                "QDRANT_COLLECTION_LONG_MEMORY", "agent_long_memory"
            ),
            embedding_dim=int(os.getenv("EMBEDDING_DIM", "1024")),
            enable_qdrant=env_bool("ENABLE_QDRANT"),
            enable_llm=env_bool("ENABLE_LLM"),
            openai_chat_model=os.getenv("OPENAI_CHAT_MODEL", "gpt-4o-mini"),
            openai_embedding_model=os.getenv(
                "OPENAI_EMBEDDING_MODEL", "text-embedding-3-small"
            ),
            openai_temperature=float(os.getenv("OPENAI_TEMPERATURE", "0")),
        )
