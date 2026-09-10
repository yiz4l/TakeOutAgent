package com.yizl.healthy.agent.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HealthAnalysisTaskResponse(
        Long id,
        String requestId,
        LocalDate analysisDate,
        Long inputRevision,
        Long profileRevision,
        String sourceRevision,
        String modelVersion,
        String status,
        int attemptCount,
        Long analysisId,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
