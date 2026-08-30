package com.yizl.healthy.agent.service;

import com.yizl.healthy.common.exception.BusinessException;
import com.yizl.healthy.common.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentTaskLeaseService {
    private final JdbcTemplate jdbc;
    private final PeriodSourceRevisionService periodRevisions;

    public AgentTaskLeaseService(JdbcTemplate jdbc, PeriodSourceRevisionService periodRevisions) {
        this.jdbc = jdbc;
        this.periodRevisions = periodRevisions;
    }

    @Transactional
    public Map<String, Object> claim(String workerId, int leaseSeconds) {
        jdbc.update("UPDATE health_analysis_task SET status=CASE WHEN attempt_count>=3 THEN 'FAILED' ELSE 'PENDING' END, lease_owner=NULL, lease_until=NULL, update_time=NOW() WHERE status='RUNNING' AND lease_until<NOW()");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM health_analysis_task WHERE status='PENDING' ORDER BY create_time,id LIMIT 1 FOR UPDATE SKIP LOCKED");
        if (rows.isEmpty()) return null;
        Map<String, Object> task = rows.get(0);
        Long taskId = ((Number) task.get("id")).longValue();
        String taskType = String.valueOf(task.get("task_type"));
        String sourceRevision = String.valueOf(task.get("source_revision"));
        if (sourceRevision.isBlank()) {
            java.sql.Date endValue = (java.sql.Date) (task.get("period_end") != null
                    ? task.get("period_end") : task.get("analysis_date"));
            sourceRevision = periodRevisions.calculate(((Number) task.get("user_id")).longValue(),
                    taskType, endValue.toLocalDate());
            jdbc.update("UPDATE health_analysis_task SET source_revision=? WHERE id=?", sourceRevision, taskId);
            task.put("source_revision", sourceRevision);
        }
        LocalDateTime leaseUntil = LocalDateTime.now().plusSeconds(leaseSeconds);
        int updated = jdbc.update("UPDATE health_analysis_task SET status='RUNNING', attempt_count=attempt_count+1, lease_owner=?, lease_until=?, update_time=NOW() WHERE id=? AND status='PENDING'",
                workerId, leaseUntil, taskId);
        if (updated != 1) throw new BusinessException(ErrorCode.DATA_CONFLICT, "任务已被其他Worker领取");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId.toString());
        result.put("requestId", task.get("request_id"));
        result.put("taskType", task.get("task_type"));
        result.put("userId", String.valueOf(task.get("user_id")));
        result.put("analysisDate", String.valueOf(task.get("analysis_date")));
        result.put("periodStart", task.get("period_start"));
        result.put("periodEnd", task.get("period_end"));
        result.put("inputRevision", String.valueOf(task.get("input_revision")));
        result.put("profileRevision", String.valueOf(task.get("profile_revision")));
        result.put("sourceRevision", sourceRevision);
        result.put("modelVersion", task.get("model_version"));
        result.put("attemptCount", ((Number) task.get("attempt_count")).intValue() + 1);
        result.put("leaseOwner", workerId);
        result.put("leaseUntil", leaseUntil);
        return result;
    }

    public Map<String, Object> heartbeat(Long taskId, String workerId, int leaseSeconds) {
        LocalDateTime leaseUntil = LocalDateTime.now().plusSeconds(leaseSeconds);
        int updated = jdbc.update("UPDATE health_analysis_task SET lease_until=?, update_time=NOW() WHERE id=? AND status='RUNNING' AND lease_owner=? AND lease_until>NOW()",
                leaseUntil, taskId, workerId);
        if (updated != 1) throw new BusinessException(ErrorCode.DATA_CONFLICT, "任务租约已失效");
        return Map.of("leaseUntil", leaseUntil);
    }

    public void fail(Long taskId, String workerId, boolean retryable, String errorMessage) {
        String status = retryable ? "PENDING" : "FAILED";
        int updated = jdbc.update("UPDATE health_analysis_task SET status=CASE WHEN ?='PENDING' AND attempt_count<3 THEN 'PENDING' ELSE 'FAILED' END, lease_owner=NULL, lease_until=NULL, error_message=?, update_time=NOW() WHERE id=? AND status='RUNNING' AND lease_owner=?",
                status, errorMessage, taskId, workerId);
        if (updated != 1) throw new BusinessException(ErrorCode.DATA_CONFLICT, "任务失败状态上报冲突");
    }
}
