package com.yizl.healthy.agent.controller;

import com.yizl.healthy.agent.dto.request.CreateHealthAnalysisTaskRequest;
import com.yizl.healthy.agent.service.HealthAnalysisTaskService;
import com.yizl.healthy.common.api.ApiResponse;
import com.yizl.healthy.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('USER')")
public class HealthAnalysisController {
    private final HealthAnalysisTaskService service;
    public HealthAnalysisController(HealthAnalysisTaskService service) { this.service = service; }

    @PostMapping("/health-analysis-tasks")
    public ApiResponse<?> create(@AuthenticationPrincipal AuthenticatedUser user, @RequestHeader(value = "Idempotency-Key", required = false) String key, @Valid @RequestBody CreateHealthAnalysisTaskRequest request) {
        return ApiResponse.success(service.create(user.userId(), key, request));
    }
    @GetMapping("/health-analysis-tasks/{taskId}")
    public ApiResponse<?> task(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long taskId) { return ApiResponse.success(service.getTask(user.userId(), taskId)); }
    @GetMapping("/health-analyses/{analysisId}")
    public ApiResponse<?> analysis(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long analysisId) { return ApiResponse.success(service.getAnalysis(user.userId(), analysisId)); }
}
