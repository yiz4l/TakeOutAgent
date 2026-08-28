from __future__ import annotations
from dataclasses import dataclass
from .schemas import HealthAnalysisContext, Recommendation

@dataclass(frozen=True)
class RuleSummary:
    calories: float; protein_g: float; fat_g: float; carbohydrate_g: float; tags: dict[str, int]; protein_deficit: bool

def calculate(context: HealthAnalysisContext) -> RuleSummary:
    totals = {"calories": 0.0, "protein_g": 0.0, "fat_g": 0.0, "carbohydrate_g": 0.0}
    for record in context.diet_records:
        for key in totals: totals[key] += getattr(record.dish_nutrition_detail, key) * max(record.quantity, 0)
    return RuleSummary(**totals, tags={x.nutrition_tag: x.count for x in context.nutrition_summary}, protein_deficit=totals["protein_g"] < 50)

def build_result(context, summary):
    score, risks = 100.0, []
    if summary.protein_deficit: score -= 20; risks.append("蛋白质摄入可能不足")
    if summary.calories == 0: score -= 10; risks.append("当天暂无可分析的饮食记录")
    suggestion = "建议下一餐优先选择优质蛋白质，并搭配蔬菜和主食。" if summary.protein_deficit else "当前记录的营养结构较均衡，后续继续保持多样化饮食。"
    recs = [Recommendation(categoryId=x.category_id, recommendationScore=90, reason="有助于改善当前营养结构") for x in context.available_recommendation_categories if summary.protein_deficit and "高蛋白" in x.nutrition_tags][:3]
    return max(score, 0), "；".join(risks) or "暂无明显风险", suggestion, recs

