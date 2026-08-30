package com.yizl.healthy.agent.controller;

import com.yizl.healthy.agent.dto.HealthAnalysisCompleteRequest;
import com.yizl.healthy.agent.service.HealthAnalysisCompletionService;
import com.yizl.healthy.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/internal/agent/health-analysis-tasks")
public class HealthAnalysisCompletionController {
    private final HealthAnalysisCompletionService service;
    private final String serviceToken;

    public HealthAnalysisCompletionController(HealthAnalysisCompletionService service,
                                              @Value("${agent.service-token:}") String serviceToken) {
        this.service = service;
        this.serviceToken = serviceToken;
    }

    @PostMapping("/{taskId}/complete")
    public ApiResponse<Map<String, Object>> complete(
            @PathVariable Long taskId,
            @RequestHeader("X-Agent-Service-Token") String token,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody HealthAnalysisCompleteRequest request) {
        if (serviceToken.isBlank() || !serviceToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid agent service credential");
        }
        return ApiResponse.success(service.complete(taskId, idempotencyKey, request));
    }
}
