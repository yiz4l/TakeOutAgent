package com.yizl.healthy.agent.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizl.healthy.agent.mapper.AgentContextMapper;
import com.yizl.healthy.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/agent/health-analysis-tasks")
public class AgentContextController {
    private final AgentContextMapper mapper;
    private final ObjectMapper objectMapper;
    private final String serviceToken;

    public AgentContextController(AgentContextMapper mapper, ObjectMapper objectMapper,
                                  @Value("${agent.service-token:}") String serviceToken) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.serviceToken = serviceToken;
    }

    @GetMapping("/{taskId}/context")
    public ApiResponse<Map<String, Object>> context(
            @PathVariable Long taskId,
            @RequestHeader("X-Agent-Service-Token") String token,
            @RequestHeader("X-Agent-Worker-Id") String workerId) {
        if (serviceToken.isBlank() || !serviceToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid agent service credential");
        }
        Map<String, Object> task = mapper.selectTask(taskId);
        if (task == null || !"RUNNING".equals(task.get("status"))
                || !workerId.equals(task.get("lease_owner"))
                || task.get("lease_until") == null
                || !((java.sql.Timestamp) task.get("lease_until")).toLocalDateTime().isAfter(java.time.LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "task lease does not belong to worker");
        }
        Long userId = ((Number) task.get("user_id")).longValue();
        LocalDate analysisDate = ((java.sql.Date) task.get("analysis_date")).toLocalDate();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("taskId", taskId.toString());
        result.put("userId", userId.toString());
        result.put("analysisDate", analysisDate.toString());
        result.put("inputRevision", String.valueOf(task.get("input_revision")));
        result.put("profileRevision", String.valueOf(task.getOrDefault("profile_revision", 0)));
        result.put("sourceRevision", String.valueOf(task.getOrDefault("source_revision", "")));
        result.put("modelVersion", task.get("model_version"));
        result.put("healthProfile", normalizeProfile(mapper.selectProfile(userId)));
        result.put("dietRecords", normalizeDietRecords(mapper.selectDietRecords(userId, analysisDate)));
        result.put("recentDailyNutrition", mapper.selectRecentNutrition(userId, analysisDate));
        result.put("priorPeriodSummaries", normalizeSummaries(mapper.selectPeriodSummaries(userId)));
        result.put("nutritionSummary", mapper.selectNutritionSummary(userId, analysisDate));
        result.put("availableRecommendationCategories", normalizeCategories(mapper.selectRecommendationCategories()));
        return ApiResponse.success(result);
    }

    private Map<String, Object> normalizeProfile(Map<String, Object> profile) {
        if (profile == null) return null;
        rename(profile, "height_cm", "heightCm");
        rename(profile, "weight_kg", "weightKg");
        rename(profile, "target_type", "targetType");
        rename(profile, "target_weight_kg", "targetWeightKg");
        rename(profile, "target_duration_weeks", "targetDurationWeeks");
        rename(profile, "activity_level", "activityLevel");
        for (String field : List.of("diet_preferences", "disliked_foods", "allergies")) {
            Object value = profile.remove(field);
            String camel = field.replace("_", "");
            if ("diet_preferences".equals(field)) camel = "dietPreferences";
            if ("disliked_foods".equals(field)) camel = "dislikedFoods";
            if ("allergies".equals(field)) camel = "allergies";
            profile.put(camel, parseList(value));
        }
        profile.put("userId", String.valueOf(profile.get("user_id")));
        profile.remove("user_id");
        profile.put("profileRevision", String.valueOf(profile.get("profile_revision")));
        profile.remove("profile_revision");
        return profile;
    }

    private void rename(Map<String, Object> map, String from, String to) {
        if (map.containsKey(from)) map.put(to, map.remove(from));
    }

    private List<Map<String, Object>> normalizeDietRecords(List<Map<String, Object>> records) {
        for (Map<String, Object> record : records) {
            Object tags = record.remove("foodNutritionTags");
            record.put("foodNutritionTags", parseList(tags));
            Object nutrition = record.remove("nutritionDetail");
            if (nutrition != null) {
                try { record.put("dishNutritionDetail", objectMapper.readValue(nutrition.toString(), new TypeReference<Map<String, Object>>() {})); }
                catch (Exception ignored) { record.put("dishNutritionDetail", Map.of()); }
            } else {
                record.put("dishNutritionDetail", Map.of());
            }
        }
        return records;
    }

    private List<Map<String, Object>> normalizeSummaries(List<Map<String, Object>> summaries) {
        for (Map<String, Object> summary : summaries) {
            Object trends = summary.remove("nutrientTrends");
            if (trends != null) {
                try { summary.put("nutrientTrends", objectMapper.readValue(trends.toString(), new TypeReference<Map<String, String>>() {})); }
                catch (Exception ignored) { summary.put("nutrientTrends", Map.of()); }
            }
        }
        return summaries;
    }

    private List<Map<String, Object>> normalizeCategories(List<Map<String, Object>> categories) {
        for (Map<String, Object> category : categories) {
            Object tags = category.get("nutritionTags");
            category.put("nutritionTags", tags == null || tags.toString().isBlank() ? List.of() : List.of(tags.toString().split(",")));
        }
        return categories;
    }

    private List<String> parseList(Object value) {
        if (value == null) return List.of();
        try { return objectMapper.readValue(value.toString(), new TypeReference<>() {}); }
        catch (Exception ignored) { return List.of(); }
    }
}
