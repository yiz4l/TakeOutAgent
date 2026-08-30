package com.yizl.healthy.agent.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface AgentContextMapper {
    @Select("SELECT id, user_id, analysis_date, input_revision, profile_revision, source_revision, model_version, status, task_type, lease_owner, lease_until FROM health_analysis_task WHERE id = #{taskId}")
    Map<String, Object> selectTask(@Param("taskId") Long taskId);

    @Select("SELECT * FROM user_health_profile WHERE user_id = #{userId}")
    Map<String, Object> selectProfile(@Param("userId") Long userId);

    @Select("SELECT dr.id, dr.dish_id AS dishId, dr.food_name AS foodName, dr.food_nutrition_tags AS foodNutritionTags, dr.meal_time AS mealTime, dr.quantity, d.nutrition_detail AS nutritionDetail FROM diet_record dr LEFT JOIN dish d ON d.id = dr.dish_id WHERE dr.user_id = #{userId} AND DATE(dr.meal_time) = #{date} ORDER BY dr.meal_time")
    List<Map<String, Object>> selectDietRecords(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT record_date AS recordDate, calories, protein_g AS proteinG, fat_g AS fatG, carbohydrate_g AS carbohydrateG, revision FROM nutrition_daily_state WHERE user_id = #{userId} AND record_date BETWEEN DATE_SUB(#{date}, INTERVAL 13 DAY) AND #{date} ORDER BY record_date")
    List<Map<String, Object>> selectRecentNutrition(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT id AS summaryId, period_type AS periodType, period_start AS periodStart, period_end AS periodEnd, summary, nutrient_trends AS nutrientTrends FROM health_period_summary WHERE user_id = #{userId} ORDER BY period_end DESC LIMIT 8")
    List<Map<String, Object>> selectPeriodSummaries(@Param("userId") Long userId);

    @Select("SELECT category_id AS categoryId, nutrition_tag AS nutritionTag, COUNT(*) AS count FROM nutrition_record WHERE user_id = #{userId} AND record_date BETWEEN DATE_SUB(#{date}, INTERVAL 13 DAY) AND #{date} GROUP BY category_id, nutrition_tag")
    List<Map<String, Object>> selectNutritionSummary(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT dc.id AS categoryId, dc.name AS categoryName, COALESCE(GROUP_CONCAT(cn.nutrition_tag), '') AS nutritionTags FROM dish_category dc LEFT JOIN category_nutrition cn ON cn.category_id = dc.id GROUP BY dc.id, dc.name")
    List<Map<String, Object>> selectRecommendationCategories();
}
