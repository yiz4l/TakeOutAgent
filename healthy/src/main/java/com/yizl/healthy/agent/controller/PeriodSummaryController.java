package com.yizl.healthy.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizl.healthy.agent.dto.PeriodSummaryCompleteRequest;
import com.yizl.healthy.agent.mapper.PeriodSummaryMapper;
import com.yizl.healthy.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/internal/agent/period-summaries")
public class PeriodSummaryController {
    private final PeriodSummaryMapper mapper;
    private final ObjectMapper objectMapper;
    private final String serviceToken;

    public PeriodSummaryController(PeriodSummaryMapper mapper, ObjectMapper objectMapper,
                                   @Value("${agent.service-token:}") String serviceToken) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.serviceToken = serviceToken;
    }

    @PostMapping("/complete")
    public ApiResponse<Map<String, Object>> complete(
            @RequestHeader("X-Agent-Service-Token") String token,
            @Valid @RequestBody PeriodSummaryCompleteRequest request) {
        if (serviceToken.isBlank() || !serviceToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid agent service credential");
        }
        try {
            Long userId = Long.valueOf(request.userId());
            mapper.upsert(null, userId, request.periodType(), LocalDate.parse(request.periodStart()),
                    LocalDate.parse(request.periodEnd()), request.sourceRevision(),
                    request.profileRevision() == null ? 0L : request.profileRevision(), request.summary(),
                    objectMapper.writeValueAsString(request.nutrientTrends() == null ? Map.of() : request.nutrientTrends()), request.modelVersion());
            return ApiResponse.success(Map.of("status", "SUCCEEDED"));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid period summary", exception);
        }
    }
}
