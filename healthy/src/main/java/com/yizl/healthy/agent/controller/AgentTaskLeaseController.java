package com.yizl.healthy.agent.controller;

import com.yizl.healthy.agent.service.AgentTaskLeaseService;
import com.yizl.healthy.common.api.ApiResponse;
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
public class AgentTaskLeaseController {
    private final AgentTaskLeaseService service;
    private final String serviceToken;

    public AgentTaskLeaseController(AgentTaskLeaseService service,
                                    @Value("${agent.service-token:}") String serviceToken) {
        this.service = service;
        this.serviceToken = serviceToken;
    }

    @PostMapping("/claim")
    public ApiResponse<Map<String, Object>> claim(@RequestHeader("X-Agent-Service-Token") String token,
                                                   @RequestBody Map<String, Object> body) {
        authenticate(token);
        String workerId = String.valueOf(body.get("workerId"));
        int leaseSeconds = Math.max(30, Math.min(600, ((Number) body.getOrDefault("leaseSeconds", 120)).intValue()));
        return ApiResponse.success(service.claim(workerId, leaseSeconds));
    }

    @PostMapping("/{taskId}/heartbeat")
    public ApiResponse<Map<String, Object>> heartbeat(@PathVariable Long taskId,
                                                       @RequestHeader("X-Agent-Service-Token") String token,
                                                       @RequestBody Map<String, Object> body) {
        authenticate(token);
        return ApiResponse.success(service.heartbeat(taskId, String.valueOf(body.get("workerId")),
                Math.max(30, Math.min(600, ((Number) body.getOrDefault("leaseSeconds", 120)).intValue()))));
    }

    @PostMapping("/{taskId}/fail")
    public ApiResponse<Void> fail(@PathVariable Long taskId,
                                  @RequestHeader("X-Agent-Service-Token") String token,
                                  @RequestBody Map<String, Object> body) {
        authenticate(token);
        service.fail(taskId, String.valueOf(body.get("workerId")),
                Boolean.TRUE.equals(body.get("retryable")), String.valueOf(body.getOrDefault("errorMessage", "")));
        return ApiResponse.success(null);
    }

    private void authenticate(String token) {
        if (serviceToken.isBlank() || !serviceToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid agent service credential");
        }
    }
}
