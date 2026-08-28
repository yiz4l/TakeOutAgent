from .schemas import AnalysisResult, HealthAnalysisContext

def resolve(result: AnalysisResult, context: HealthAnalysisContext, fallback: AnalysisResult) -> AnalysisResult:
    """Model output is a candidate; Java facts and deterministic rules win conflicts."""
    allowed = {item.category_id for item in context.available_recommendation_categories}
    recommendations = [item for item in result.recommendations if item.category_id in allowed] or fallback.recommendations
    return result.model_copy(update={"recommendations": recommendations, "health_score": fallback.health_score, "risk_summary": fallback.risk_summary})

