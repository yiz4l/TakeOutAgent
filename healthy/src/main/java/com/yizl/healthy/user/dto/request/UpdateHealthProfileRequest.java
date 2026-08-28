package com.yizl.healthy.user.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public record UpdateHealthProfileRequest(
        @DecimalMin("50") @DecimalMax("260") BigDecimal heightCm,
        @DecimalMin("10") @DecimalMax("500") BigDecimal weightKg,
        @NotBlank @Pattern(regexp = "MUSCLE_GAIN|FAT_LOSS|MAINTAIN") String targetType,
        @DecimalMin("10") @DecimalMax("500") BigDecimal targetWeightKg,
        @Min(1) @Max(260) Integer targetDurationWeeks,
        @Pattern(regexp = "SEDENTARY|LIGHT|MODERATE|HIGH|VERY_HIGH") String activityLevel,
        List<String> dietPreferences,
        List<String> dislikedFoods,
        List<String> allergies
) {
}
