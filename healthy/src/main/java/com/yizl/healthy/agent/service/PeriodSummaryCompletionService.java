package com.yizl.healthy.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizl.healthy.agent.dto.PeriodSummaryCompleteRequest;
import com.yizl.healthy.common.exception.BusinessException;
import com.yizl.healthy.common.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class PeriodSummaryCompletionService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final PeriodSourceRevisionService periodRevisions;

    public PeriodSummaryCompletionService(JdbcTemplate jdbc, ObjectMapper objectMapper,
                                          PeriodSourceRevisionService periodRevisions) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.periodRevisions = periodRevisions;
    }

    @Transactional
    public Map<String, Object> complete(String idempotencyKey, PeriodSummaryCompleteRequest request) {
        Long taskId = Long.valueOf(request.taskId());
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM health_analysis_task WHERE id=? FOR UPDATE", taskId);
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "周期总结任务不存在");
        Map<String, Object> task = rows.get(0);
        if (!idempotencyKey.equals(task.get("request_id"))) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "幂等键与任务不匹配");
        }
        if ("SUCCEEDED".equals(task.get("status"))) {
            return response(taskId, ((Number) task.get("period_summary_id")).longValue());
        }
        validate(task, request);
        Long userId = ((Number) task.get("user_id")).longValue();
        Long currentProfileRevision = jdbc.queryForObject(
                "SELECT COALESCE(MAX(profile_revision),0) FROM user_health_profile WHERE user_id=?",
                Long.class, userId);
        String currentSourceRevision = periodRevisions.calculate(userId,
                String.valueOf(task.get("task_type")), LocalDate.parse(request.periodEnd()));
        if (!String.valueOf(task.get("profile_revision")).equals(String.valueOf(currentProfileRevision))
                || !request.sourceRevision().equals(task.get("source_revision"))
                || !request.sourceRevision().equals(currentSourceRevision)) {
            jdbc.update("UPDATE health_analysis_task SET status='STALE', lease_owner=NULL, lease_until=NULL, update_time=NOW() WHERE id=? AND status='RUNNING'", taskId);
            return Map.of("taskId", taskId.toString(), "status", "STALE");
        }
        Long summaryId = upsertSummary(request);
        int updated = jdbc.update("UPDATE health_analysis_task SET status='SUCCEEDED', period_summary_id=?, lease_owner=NULL, lease_until=NULL, error_message=NULL, update_time=NOW() WHERE id=? AND status='RUNNING' AND lease_owner=?",
                summaryId, taskId, request.workerId());
        if (updated != 1) throw new BusinessException(ErrorCode.DATA_CONFLICT, "周期任务状态已变化");
        return response(taskId, summaryId);
    }

    private void validate(Map<String, Object> task, PeriodSummaryCompleteRequest request) {
        Timestamp leaseUntil = (Timestamp) task.get("lease_until");
        String expectedTaskType = "BIWEEKLY".equals(request.periodType()) ? "BIWEEKLY_SUMMARY" : "MONTHLY_SUMMARY";
        boolean valid = "RUNNING".equals(task.get("status"))
                && expectedTaskType.equals(task.get("task_type"))
                && request.workerId().equals(task.get("lease_owner"))
                && leaseUntil != null && leaseUntil.toLocalDateTime().isAfter(LocalDateTime.now())
                && request.userId().equals(String.valueOf(task.get("user_id")))
                && request.modelVersion().equals(task.get("model_version"));
        if (!valid) throw new BusinessException(ErrorCode.DATA_CONFLICT, "周期任务租约、类型或版本无效");
        LocalDate taskStart = ((java.sql.Date) task.get("period_start")).toLocalDate();
        LocalDate taskEnd = ((java.sql.Date) task.get("period_end")).toLocalDate();
        if (!taskStart.equals(LocalDate.parse(request.periodStart())) || !taskEnd.equals(LocalDate.parse(request.periodEnd()))) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "周期范围与任务不匹配");
        }
    }

    private Long upsertSummary(PeriodSummaryCompleteRequest request) {
        String trends;
        try {
            trends = objectMapper.writeValueAsString(request.nutrientTrends() == null ? Map.of() : request.nutrientTrends());
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "周期营养趋势格式错误");
        }
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO health_period_summary(user_id, period_type, period_start, period_end, source_revision, profile_revision, summary, nutrient_trends, model_version) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id), summary=VALUES(summary), nutrient_trends=VALUES(nutrient_trends), update_time=NOW()",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, Long.parseLong(request.userId()));
            statement.setString(2, request.periodType());
            statement.setObject(3, LocalDate.parse(request.periodStart()));
            statement.setObject(4, LocalDate.parse(request.periodEnd()));
            statement.setString(5, request.sourceRevision());
            statement.setLong(6, request.profileRevision() == null ? 0L : request.profileRevision());
            statement.setString(7, request.summary());
            statement.setString(8, trends);
            statement.setString(9, request.modelVersion());
            return statement;
        }, keys);
        if (keys.getKey() == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "周期总结ID生成失败");
        return keys.getKey().longValue();
    }

    private Map<String, Object> response(Long taskId, Long summaryId) {
        return Map.of("taskId", taskId.toString(), "status", "SUCCEEDED", "periodSummaryId", summaryId.toString());
    }
}
