package com.yizl.healthy.user.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record HealthProfileResponse(
        String userId,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String targetType,
        BigDecimal targetWeightKg,
        Integer targetDurationWeeks,
        String activityLevel,
        List<String> dietPreferences,
        List<String> dislikedFoods,
        List<String> allergies,
        String profileRevision
) {
}
