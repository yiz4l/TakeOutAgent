package com.yizl.healthy.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PeriodSummaryCompleteRequest(
        @NotBlank String workerId,
        @NotBlank @Pattern(regexp = "BIWEEKLY|MONTHLY") String periodType,
        @NotBlank String userId,
        @NotBlank String periodStart,
        @NotBlank String periodEnd,
        @NotBlank @Size(max = 10000) String summary,
        Map<String, String> nutrientTrends,
        String sourceRevision,
        Long profileRevision,
        @NotBlank String modelVersion
) {
}
