from __future__ import annotations
from datetime import date, datetime
from pydantic import BaseModel, ConfigDict, Field, field_validator


class NutritionDetail(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    calories: float = 0
    protein_g: float = Field(default=0, alias="proteinG")
    fat_g: float = Field(default=0, alias="fatG")
    carbohydrate_g: float = Field(default=0, alias="carbohydrateG")


class DietRecord(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    id: str
    dish_id: str | None = Field(default=None, alias="dishId")
    food_name: str | None = Field(default=None, alias="foodName")
    food_nutrition_tags: list[str] = Field(
        default_factory=list, alias="foodNutritionTags"
    )
    meal_time: datetime | None = Field(default=None, alias="mealTime")
    quantity: float = 1
    dish_nutrition_detail: NutritionDetail = Field(
        default_factory=NutritionDetail, alias="dishNutritionDetail"
    )


class NutritionTagCount(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    category_id: str | None = None
    nutrition_tag: str
    count: int = 0


class RecommendationCategory(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    category_id: str
    category_name: str
    nutrition_tags: list[str] = Field(default_factory=list)


class HealthProfile(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    height_cm: float | None = Field(default=None, alias="heightCm")
    weight_kg: float | None = Field(default=None, alias="weightKg")
    target_type: str = Field(default="MAINTAIN", alias="targetType")
    target_weight_kg: float | None = Field(default=None, alias="targetWeightKg")
    target_duration_weeks: int | None = Field(default=None, alias="targetDurationWeeks")
    activity_level: str | None = Field(default=None, alias="activityLevel")
    diet_preferences: list[str] = Field(default_factory=list, alias="dietPreferences")
    disliked_foods: list[str] = Field(default_factory=list, alias="dislikedFoods")
    allergies: list[str] = Field(default_factory=list)


class DailyNutrition(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    record_date: date = Field(alias="recordDate")
    calories: float = 0
    protein_g: float = Field(default=0, alias="proteinG")
    fat_g: float = Field(default=0, alias="fatG")
    carbohydrate_g: float = Field(default=0, alias="carbohydrateG")
    revision: int = 0


class PeriodSummary(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    summary_id: str | None = Field(default=None, alias="summaryId")
    period_type: str = Field(alias="periodType")
    period_start: date = Field(alias="periodStart")
    period_end: date = Field(alias="periodEnd")
    summary: str
    nutrient_trends: dict[str, str] = Field(
        default_factory=dict, alias="nutrientTrends"
    )


class HealthAnalysisContext(BaseModel):
    model_config = ConfigDict(populate_by_name=True, protected_namespaces=())
    task_id: str = Field(alias="taskId")
    user_id: str = Field(alias="userId")
    analysis_date: date = Field(alias="analysisDate")
    input_revision: str = Field(alias="inputRevision")
    model_version: str = Field(alias="modelVersion")
    nutrition_summary: list[NutritionTagCount] = Field(
        default_factory=list, alias="nutritionSummary"
    )
    diet_records: list[DietRecord] = Field(default_factory=list, alias="dietRecords")
    available_recommendation_categories: list[RecommendationCategory] = Field(
        default_factory=list, alias="availableRecommendationCategories"
    )
    health_profile: HealthProfile | None = Field(default=None, alias="healthProfile")
    recent_daily_nutrition: list[DailyNutrition] = Field(
        default_factory=list, alias="recentDailyNutrition"
    )
    prior_period_summaries: list[PeriodSummary] = Field(
        default_factory=list, alias="priorPeriodSummaries"
    )
    profile_revision: str = Field(default="0", alias="profileRevision")
    source_revision: str = Field(default="", alias="sourceRevision")


class TaskLease(BaseModel):
    model_config = ConfigDict(populate_by_name=True, protected_namespaces=())
    task_id: str = Field(alias="taskId")
    request_id: str = Field(alias="requestId")
    user_id: str = Field(alias="userId")
    analysis_date: date = Field(alias="analysisDate")
    input_revision: str = Field(alias="inputRevision")
    model_version: str = Field(alias="modelVersion")
    attempt_count: int = Field(alias="attemptCount")
    task_type: str = Field(default="DAILY_ANALYSIS", alias="taskType")
    period_start: date | None = Field(default=None, alias="periodStart")
    period_end: date | None = Field(default=None, alias="periodEnd")
    profile_revision: str = Field(default="0", alias="profileRevision")
    source_revision: str = Field(default="", alias="sourceRevision")


class Recommendation(BaseModel):
    category_id: str = Field(alias="categoryId")
    recommendation_score: float = Field(alias="recommendationScore", ge=0, le=100)
    reason: str = Field(min_length=1, max_length=500)


class AnalysisResult(BaseModel):
    health_score: float = Field(ge=0, le=100, alias="healthScore")
    risk_summary: str = Field(max_length=1000, alias="riskSummary")
    optimization_suggestion: str = Field(
        max_length=2000, alias="optimizationSuggestion"
    )
    recommendations: list[Recommendation] = Field(default_factory=list)

    @field_validator("recommendations")
    @classmethod
    def unique_categories(cls, value):
        ids = [x.category_id for x in value]
        if len(ids) != len(set(ids)):
            raise ValueError("recommendation categories must be unique")
        return value
