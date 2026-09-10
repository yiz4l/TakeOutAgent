from __future__ import annotations

from dataclasses import dataclass

from .schemas import HealthAnalysisContext, Recommendation


@dataclass(frozen=True)
class RuleSummary:
    calories: float
    protein_g: float
    fat_g: float
    carbohydrate_g: float
    tags: dict[str, int]
    protein_target_g: float
    protein_deficit: bool
    recent_protein_average_g: float
    recent_protein_deficit_days: int
    blocked_foods: tuple[str, ...]


def calculate(context: HealthAnalysisContext) -> RuleSummary:
    totals = {"calories": 0.0, "protein_g": 0.0, "fat_g": 0.0, "carbohydrate_g": 0.0}
    for record in context.diet_records:
        for key in totals:
            totals[key] += getattr(record.dish_nutrition_detail, key) * max(
                record.quantity, 0
            )

    profile = context.health_profile
    weight = profile.weight_kg if profile and profile.weight_kg else 62.5
    target_type = profile.target_type if profile else "MAINTAIN"
    factor = {"MUSCLE_GAIN": 1.6, "FAT_LOSS": 1.2, "MAINTAIN": 0.8}.get(
        target_type, 0.8
    )
    protein_target = round(weight * factor, 1)
    recent = context.recent_daily_nutrition
    recent_average = (
        round(sum(day.protein_g for day in recent) / len(recent), 1) if recent else 0.0
    )
    deficit_days = sum(day.protein_g < protein_target for day in recent)
    blocked = tuple(
        dict.fromkeys((profile.allergies + profile.disliked_foods) if profile else [])
    )
    return RuleSummary(
        **totals,
        tags={item.nutrition_tag: item.count for item in context.nutrition_summary},
        protein_target_g=protein_target,
        protein_deficit=totals["protein_g"] < protein_target,
        recent_protein_average_g=recent_average,
        recent_protein_deficit_days=deficit_days,
        blocked_foods=blocked,
    )


def build_result(context: HealthAnalysisContext, summary: RuleSummary):
    score = 100.0
    risks: list[str] = []
    if summary.protein_deficit:
        score -= 20
        risks.append(f"蛋白质摄入低于当前目标{summary.protein_target_g:g}克")
    if summary.recent_protein_deficit_days >= 7:
        score -= 10
        risks.append(
            f"最近{len(context.recent_daily_nutrition)}天有{summary.recent_protein_deficit_days}天蛋白质不足"
        )
    if summary.calories == 0:
        score -= 10
        risks.append("当天暂无可分析的饮食记录")
    suggestion = (
        "建议下一餐补充符合个人忌口和过敏限制的优质蛋白质，并搭配蔬菜和主食。"
        if summary.protein_deficit
        else "当前蛋白质达到规则目标，后续继续保持饮食多样性。"
    )
    blocked = tuple(item.lower() for item in summary.blocked_foods)
    recommendations = []
    for category in context.available_recommendation_categories:
        searchable = (
            f"{category.category_name} {' '.join(category.nutrition_tags)}".lower()
        )
        if blocked and any(food.lower() in searchable for food in blocked):
            continue
        if summary.protein_deficit and "高蛋白" in category.nutrition_tags:
            recommendations.append(
                Recommendation(
                    categoryId=category.category_id,
                    recommendationScore=90,
                    reason="有助于补充当前蛋白质缺口",
                )
            )
    return (
        max(score, 0),
        "；".join(risks) or "暂无明显风险",
        suggestion,
        recommendations[:3],
    )
