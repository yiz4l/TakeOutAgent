from .schemas import AnalysisResult, HealthAnalysisContext

def resolve(result: AnalysisResult, context: HealthAnalysisContext, fallback: AnalysisResult) -> AnalysisResult:
    """Model output is a candidate; Java facts and deterministic rules win conflicts."""
    allowed = {item.category_id for item in context.available_recommendation_categories}
    categories = {item.category_id: item for item in context.available_recommendation_categories}
    profile = context.health_profile
    blocked = [item.lower() for item in ((profile.allergies + profile.disliked_foods) if profile else [])]
    recommendations = []
    for item in result.recommendations:
        category = categories.get(item.category_id)
        searchable = f"{category.category_name} {' '.join(category.nutrition_tags)}".lower() if category else ""
        if item.category_id in allowed and not any(food in searchable for food in blocked):
            recommendations.append(item)
    recommendations = recommendations or fallback.recommendations
    suggestion = result.optimization_suggestion
    if any(food in suggestion.lower() for food in blocked):
        suggestion = fallback.optimization_suggestion
    return result.model_copy(update={
        "recommendations": recommendations,
        "health_score": fallback.health_score,
        "risk_summary": fallback.risk_summary,
        "optimization_suggestion": suggestion,
    })
