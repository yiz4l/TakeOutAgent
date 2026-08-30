"""Create the Qdrant collections used by the Python Agent.

Run after starting Qdrant: ``python init_qdrant.py``.
The script is idempotent and never deletes existing collections.
"""
from __future__ import annotations

import os
from qdrant_client import QdrantClient, models
from dotenv import load_dotenv


def client() -> QdrantClient:
    load_dotenv()
    kwargs = {"url": os.getenv("QDRANT_URL", "http://localhost:6333")}
    if os.getenv("QDRANT_API_KEY"):
        kwargs["api_key"] = os.environ["QDRANT_API_KEY"]
    return QdrantClient(**kwargs)


def ensure_collection(qdrant: QdrantClient, name: str, dimension: int) -> None:
    if qdrant.collection_exists(name):
        return
    qdrant.create_collection(
        collection_name=name,
        vectors_config=models.VectorParams(
            size=dimension,
            distance=models.Distance.COSINE,
            on_disk=True,
        ),
        hnsw_config=models.HnswConfigDiff(m=16, ef_construct=128),
        optimizers_config=models.OptimizersConfigDiff(indexing_threshold=20_000),
    )


def create_payload_indexes(qdrant: QdrantClient, name: str) -> None:
    # These fields are used for tenant isolation and memory lifecycle queries.
    fields = {
        "user_id": models.PayloadSchemaType.KEYWORD,
        "memory_type": models.PayloadSchemaType.KEYWORD,
        "source": models.PayloadSchemaType.KEYWORD,
        "conversation_id": models.PayloadSchemaType.KEYWORD,
        "memory_key": models.PayloadSchemaType.KEYWORD,
        "analysis_date": models.PayloadSchemaType.KEYWORD,
        "valid_until": models.PayloadSchemaType.DATETIME,
        "confidence": models.PayloadSchemaType.FLOAT,
    }
    for field, schema in fields.items():
        qdrant.create_payload_index(collection_name=name, field_name=field, field_schema=schema)


def main() -> None:
    qdrant = client()
    dimension = int(os.getenv("EMBEDDING_DIM", "1536"))
    short_memory = os.getenv("QDRANT_COLLECTION_SHORT_MEMORY", "agent_short_memory")
    long_memory = os.getenv("QDRANT_COLLECTION_LONG_MEMORY", "agent_long_memory")
    knowledge = os.getenv("QDRANT_COLLECTION_KNOWLEDGE", "nutrition_knowledge")
    for name in (short_memory, long_memory, knowledge):
        ensure_collection(qdrant, name, dimension)
        create_payload_indexes(qdrant, name)
    print(f"Qdrant ready: {short_memory}, {long_memory}, {knowledge} (dimension={dimension}, cosine)")


if __name__ == "__main__":
    main()
