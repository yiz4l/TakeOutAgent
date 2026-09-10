from __future__ import annotations
from datetime import datetime, timezone
from uuid import NAMESPACE_URL, uuid4, uuid5
from .config import Settings
from .rules import RuleSummary
from .schemas import HealthAnalysisContext
from .schemas import PeriodSummary


class MemoryStore:
    """Separate short-term conversation memory from durable user habits."""

    def __init__(self, settings: Settings):
        self.settings, self.client, self.embeddings = settings, None, None
        if settings.enable_qdrant:
            from qdrant_client import QdrantClient

            self.client = QdrantClient(
                url=settings.qdrant_url, api_key=settings.qdrant_api_key
            )
            if settings.enable_llm:
                from langchain_openai import OpenAIEmbeddings

                self.embeddings = OpenAIEmbeddings(
                    model=settings.openai_embedding_model
                )

    def recall(
        self, user_id: str, query: str, memory_type: str = "long", limit: int = 5
    ):
        if not self.client or not self.embeddings:
            return []
        from qdrant_client import models

        collection = (
            self.settings.long_memory_collection
            if memory_type == "long"
            else self.settings.short_memory_collection
        )
        points = self.client.query_points(
            collection,
            query=self.embeddings.embed_query(query),
            query_filter=models.Filter(
                must=[
                    models.FieldCondition(
                        key="user_id", match=models.MatchValue(value=user_id)
                    )
                ]
            ),
            limit=limit,
        ).points
        return [p.payload or {} for p in points]

    def remember(
        self,
        context: HealthAnalysisContext,
        summary: RuleSummary,
        text: str,
        memory_type: str = "long",
    ):
        if not self.client or not self.embeddings:
            return
        from qdrant_client import models

        collection = (
            self.settings.long_memory_collection
            if memory_type == "long"
            else self.settings.short_memory_collection
        )
        payload = {
            "user_id": context.user_id,
            "memory_type": memory_type,
            "memory_key": (
                "protein:weekly" if summary.protein_deficit else "nutrition:stable"
            ),
            "text": text,
            "structured": {
                "protein_deficit": summary.protein_deficit,
                "protein_g": summary.protein_g,
            },
            "source": "analysis",
            "analysis_date": context.analysis_date.isoformat(),
            "confidence": 0.8,
            "created_at": datetime.now(timezone.utc).isoformat(),
        }
        self.client.upsert(
            collection,
            [
                models.PointStruct(
                    id=str(uuid4()),
                    vector=self.embeddings.embed_query(text),
                    payload=payload,
                )
            ],
        )

    def remember_period(self, user_id: str, summary: PeriodSummary, model_version: str):
        if not self.client or not self.embeddings:
            return
        from qdrant_client import models

        memory_key = (
            f"period:{summary.period_type}:{summary.period_start}:{summary.period_end}"
        )
        payload = {
            "user_id": user_id,
            "memory_type": "long",
            "memory_key": memory_key,
            "text": summary.summary,
            "structured": summary.nutrient_trends,
            "source": "period_summary",
            "analysis_date": summary.period_end.isoformat(),
            "confidence": 0.9,
            "model_version": model_version,
            "created_at": datetime.now(timezone.utc).isoformat(),
        }
        point_id = str(uuid5(NAMESPACE_URL, f"{user_id}:{memory_key}:{model_version}"))
        self.client.upsert(
            self.settings.long_memory_collection,
            [
                models.PointStruct(
                    id=point_id,
                    vector=self.embeddings.embed_query(summary.summary),
                    payload=payload,
                )
            ],
        )
