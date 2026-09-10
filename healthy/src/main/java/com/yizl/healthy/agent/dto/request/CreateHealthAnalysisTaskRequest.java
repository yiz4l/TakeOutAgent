package com.yizl.healthy.agent.dto.request;

import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record CreateHealthAnalysisTaskRequest(
        @PastOrPresent LocalDate analysisDate
) {
}
