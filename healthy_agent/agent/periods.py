from __future__ import annotations

from datetime import date, timedelta

from .schemas import DailyNutrition, HealthAnalysisContext, PeriodSummary


def period_boundaries(end: date, weeks: int = 2) -> tuple[date, date]:
    return end - timedelta(days=weeks * 7 - 1), end


def build_period_prompt(context: HealthAnalysisContext, period_type: str) -> str:
    """Create compact, deterministic input for a biweekly/monthly summary task."""
    if period_type == "MONTHLY":
        source = [item.model_dump(by_alias=True) for item in context.prior_period_summaries]
        return f"用户画像={context.health_profile.model_dump(by_alias=True) if context.health_profile else {}}\n两周总结={source}"
    days = [item.model_dump(by_alias=True) for item in context.recent_daily_nutrition]
    return f"用户画像={context.health_profile.model_dump(by_alias=True) if context.health_profile else {}}\n最近每日营养={days}"


def trend(days: list[DailyNutrition]) -> dict[str, str]:
    if not days:
        return {}
    avg = lambda key: sum(getattr(day, key) for day in days) / len(days)
    return {
        "protein": "LOW" if avg("protein_g") < 50 else "NORMAL",
        "fat": "HIGH" if avg("fat_g") > 70 else "NORMAL",
        "carbohydrate": "HIGH" if avg("carbohydrate_g") > 300 else "NORMAL",
    }


def fallback_summary(context: HealthAnalysisContext, period_type: str) -> PeriodSummary:
    start, end = period_boundaries(context.analysis_date, 4 if period_type == "MONTHLY" else 2)
    trends = trend(context.recent_daily_nutrition)
    labels = {"LOW": "偏低", "HIGH": "偏高", "NORMAL": "正常"}
    text = "、".join(f"{key}{labels.get(value, value)}" for key, value in trends.items()) or "暂无足够营养数据"
    return PeriodSummary(periodType=period_type, periodStart=start, periodEnd=end, summary=f"本周期营养趋势：{text}。", nutrientTrends=trends)

