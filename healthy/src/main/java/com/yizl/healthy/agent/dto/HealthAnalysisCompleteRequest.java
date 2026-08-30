package com.yizl.healthy.agent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record HealthAnalysisCompleteRequest(
        @NotBlank String workerId,
        @NotBlank String inputRevision,
        @NotBlank String sourceRevision,
        @NotBlank String modelVersion,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal healthScore,
        @Size(max = 1000) String riskSummary,
        @Size(max = 10000) String optimizationSuggestion,
        List<@Valid RecommendationItem> recommendations
) {
    public record RecommendationItem(
            @NotBlank String categoryId,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal recommendationScore,
            @Size(max = 1000) String reason
    ) {
    }
}
