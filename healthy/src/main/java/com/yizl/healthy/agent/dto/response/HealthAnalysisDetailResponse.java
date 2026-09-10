package com.yizl.healthy.agent.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HealthAnalysisDetailResponse(
        Long id,
        LocalDate analysisDate,
        Long inputRevision,
        BigDecimal healthScore,
        String riskSummary,
        String optimizationSuggestion,
        String analysisModel,
        List<Recommendation> recommendations,
        LocalDateTime createTime
) {
    public record Recommendation(
            Long categoryId,
            String categoryName,
            BigDecimal recommendationScore,
            String reason
    ) {}
}
