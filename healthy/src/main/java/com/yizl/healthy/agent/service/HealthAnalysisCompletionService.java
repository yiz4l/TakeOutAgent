package com.yizl.healthy.agent.service;

import com.yizl.healthy.agent.dto.HealthAnalysisCompleteRequest;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HealthAnalysisCompletionService {
    private final JdbcTemplate jdbc;
    private final PeriodSourceRevisionService sourceRevisions;

    public HealthAnalysisCompletionService(JdbcTemplate jdbc,
                                           PeriodSourceRevisionService sourceRevisions) {
        this.jdbc = jdbc;
        this.sourceRevisions = sourceRevisions;
    }

    @Transactional
    public Map<String, Object> complete(Long taskId, String idempotencyKey,
                                        HealthAnalysisCompleteRequest request) {
        Map<String, Object> task = lockTask(taskId);
        if (!idempotencyKey.equals(task.get("request_id"))) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "幂等键与任务不匹配");
        }
        if ("SUCCEEDED".equals(task.get("status"))) {
            return response(taskId, ((Number) task.get("health_analysis_id")).longValue());
        }
        validateRunningTask(task, request.workerId(), request.inputRevision(), request.modelVersion(), "DAILY_ANALYSIS");

        Long userId = ((Number) task.get("user_id")).longValue();
        Long expectedRevision = ((Number) task.get("input_revision")).longValue();
        Long expectedProfileRevision = ((Number) task.get("profile_revision")).longValue();
        Long currentProfileRevision = jdbc.queryForObject(
                "SELECT COALESCE(MAX(profile_revision),0) FROM user_health_profile WHERE user_id=?",
                Long.class, userId);
        if (!expectedProfileRevision.equals(currentProfileRevision)) {
            jdbc.update("UPDATE health_analysis_task SET status='STALE', lease_owner=NULL, lease_until=NULL, update_time=NOW() WHERE id=? AND status='RUNNING'", taskId);
            return Map.of("taskId", taskId.toString(), "status", "STALE");
        }
        String currentSourceRevision = sourceRevisions.calculate(
                userId,
                "DAILY_ANALYSIS",
                ((java.sql.Date) task.get("analysis_date")).toLocalDate()
        );
        if (!request.sourceRevision().equals(task.get("source_revision"))
                || !request.sourceRevision().equals(currentSourceRevision)) {
            jdbc.update("UPDATE health_analysis_task SET status='STALE', lease_owner=NULL, lease_until=NULL, update_time=NOW() WHERE id=? AND status='RUNNING'", taskId);
            return Map.of("taskId", taskId.toString(), "status", "STALE");
        }
        Long currentRevision = jdbc.query(
                "SELECT revision FROM nutrition_daily_state WHERE user_id=? AND record_date=?",
                result -> result.next() ? result.getLong(1) : 0L,
                userId, task.get("analysis_date"));
        if (!expectedRevision.equals(currentRevision)) {
            jdbc.update("UPDATE health_analysis_task SET status='STALE', lease_owner=NULL, lease_until=NULL, update_time=NOW() WHERE id=? AND status='RUNNING'", taskId);
            return Map.of("taskId", taskId.toString(), "status", "STALE");
        }
        validateRecommendations(request.recommendations());
        Long analysisId = insertAnalysis(task, request);
        insertRecommendations(userId, analysisId, request.recommendations());
        int updated = jdbc.update("UPDATE health_analysis_task SET status='SUCCEEDED', health_analysis_id=?, lease_owner=NULL, lease_until=NULL, error_message=NULL, update_time=NOW() WHERE id=? AND status='RUNNING' AND lease_owner=?",
                analysisId, taskId, request.workerId());
        if (updated != 1) throw new BusinessException(ErrorCode.DATA_CONFLICT, "任务状态已变化");
        return response(taskId, analysisId);
    }

    private Map<String, Object> lockTask(Long taskId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM health_analysis_task WHERE id=? FOR UPDATE", taskId);
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "健康分析任务不存在");
        return rows.get(0);
    }

    private void validateRunningTask(Map<String, Object> task, String workerId, String inputRevision,
                                     String modelVersion, String taskType) {
        Timestamp leaseUntil = (Timestamp) task.get("lease_until");
        boolean valid = "RUNNING".equals(task.get("status"))
                && taskType.equals(task.get("task_type"))
                && workerId.equals(task.get("lease_owner"))
                && leaseUntil != null && leaseUntil.toLocalDateTime().isAfter(LocalDateTime.now())
                && inputRevision.equals(String.valueOf(task.get("input_revision")))
                && modelVersion.equals(task.get("model_version"));
        if (!valid) throw new BusinessException(ErrorCode.DATA_CONFLICT, "任务租约、版本或类型无效");
    }

    private void validateRecommendations(List<HealthAnalysisCompleteRequest.RecommendationItem> items) {
        if (items == null) return;
        Set<Long> ids = new HashSet<>();
        for (HealthAnalysisCompleteRequest.RecommendationItem item : items) {
            Long categoryId = Long.valueOf(item.categoryId());
            if (!ids.add(categoryId)) throw new BusinessException(ErrorCode.BAD_REQUEST, "推荐分类不能重复");
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM dish_category WHERE id=?", Integer.class, categoryId);
            if (count == null || count == 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "推荐分类不存在");
        }
    }

    private Long insertAnalysis(Map<String, Object> task, HealthAnalysisCompleteRequest request) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO health_analysis(user_id, analysis_date, input_revision, health_score, risk_summary, optimization_suggestion, analysis_model) VALUES(?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setObject(1, task.get("user_id"));
            statement.setObject(2, task.get("analysis_date"));
            statement.setObject(3, task.get("input_revision"));
            statement.setBigDecimal(4, request.healthScore());
            statement.setString(5, request.riskSummary());
            statement.setString(6, request.optimizationSuggestion());
            statement.setString(7, request.modelVersion());
            return statement;
        }, keys);
        if (keys.getKey() == null) throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分析结果ID生成失败");
        return keys.getKey().longValue();
    }

    private void insertRecommendations(Long userId, Long analysisId,
                                       List<HealthAnalysisCompleteRequest.RecommendationItem> items) {
        if (items == null) return;
        for (HealthAnalysisCompleteRequest.RecommendationItem item : items) {
            jdbc.update("INSERT INTO diet_recommendation(user_id, health_analysis_id, recommended_category_id, recommendation_score, reason) VALUES(?,?,?,?,?)",
                    userId, analysisId, Long.valueOf(item.categoryId()), item.recommendationScore(), item.reason());
        }
    }

    private Map<String, Object> response(Long taskId, Long analysisId) {
        return Map.of("taskId", taskId.toString(), "status", "SUCCEEDED", "analysisId", analysisId.toString());
    }
}
