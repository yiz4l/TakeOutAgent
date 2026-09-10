package com.yizl.healthy.agent.service;

import com.yizl.healthy.agent.dto.request.CreateHealthAnalysisTaskRequest;
import com.yizl.healthy.agent.dto.response.HealthAnalysisDetailResponse;
import com.yizl.healthy.agent.dto.response.HealthAnalysisTaskResponse;
import com.yizl.healthy.common.exception.BusinessException;
import com.yizl.healthy.common.exception.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HealthAnalysisTaskService {
    private static final String MODEL_VERSION = "diet-agent-v1";
    private final JdbcTemplate jdbc;
    private final PeriodSourceRevisionService sourceRevisions;

    public HealthAnalysisTaskService(JdbcTemplate jdbc, PeriodSourceRevisionService sourceRevisions) {
        this.jdbc = jdbc;
        this.sourceRevisions = sourceRevisions;
    }

    @Transactional
    public HealthAnalysisTaskResponse create(Long userId, String requestId, CreateHealthAnalysisTaskRequest request) {
        String effectiveRequestId = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        LocalDate date = request.analysisDate() == null ? LocalDate.now() : request.analysisDate();
        Long inputRevision = currentInputRevision(userId, date);
        Long profileRevision = currentProfileRevision(userId);
        String sourceRevision = sourceRevisions.calculate(userId, "DAILY_ANALYSIS", date);

        Map<String, Object> existing = findByInput(userId, date, inputRevision, profileRevision, sourceRevision);
        if (existing != null) return toTask(existing);
        existing = findByRequest(requestId);
        if (existing != null) {
            if (!userId.equals(((Number) existing.get("user_id")).longValue())) {
                throw new BusinessException(ErrorCode.DATA_CONFLICT, "请求幂等键已被使用");
            }
            return toTask(existing);
        }

        try {
            jdbc.update("INSERT INTO health_analysis_task(request_id,user_id,task_type,analysis_date,input_revision,profile_revision,source_revision,model_version,status,attempt_count) VALUES(?,?,?,?,?,?,?,?, 'PENDING',0)",
                    effectiveRequestId,
                    userId, "DAILY_ANALYSIS", date, inputRevision, profileRevision, sourceRevision, MODEL_VERSION);
        } catch (DuplicateKeyException duplicate) {
            Map<String, Object> retry = findByInput(userId, date, inputRevision, profileRevision, sourceRevision);
            if (retry != null) return toTask(retry);
            retry = findByRequest(effectiveRequestId);
            if (retry != null && userId.equals(((Number) retry.get("user_id")).longValue())) return toTask(retry);
            throw duplicate;
        }
        return toTask(findByRequest(effectiveRequestId));
    }

    public HealthAnalysisTaskResponse getTask(Long userId, Long taskId) {
        Map<String, Object> row = one("SELECT * FROM health_analysis_task WHERE id=? AND user_id=?", taskId, userId);
        if (row == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分析任务不存在");
        return toTask(row);
    }

    public HealthAnalysisDetailResponse getAnalysis(Long userId, Long analysisId) {
        Map<String, Object> row = one("SELECT * FROM health_analysis WHERE id=? AND user_id=?", analysisId, userId);
        if (row == null) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "分析结果不存在");
        List<HealthAnalysisDetailResponse.Recommendation> recommendations = jdbc.query(
                "SELECT dr.recommended_category_id, dc.name, dr.recommendation_score, dr.reason FROM diet_recommendation dr LEFT JOIN dish_category dc ON dc.id=dr.recommended_category_id WHERE dr.health_analysis_id=? AND dr.user_id=? ORDER BY dr.recommendation_score DESC",
                (rs, n) -> new HealthAnalysisDetailResponse.Recommendation(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4)), analysisId, userId);
        return new HealthAnalysisDetailResponse(
                ((Number) row.get("id")).longValue(), ((java.sql.Date) row.get("analysis_date")).toLocalDate(),
                ((Number) row.get("input_revision")).longValue(), (java.math.BigDecimal) row.get("health_score"),
                (String) row.get("risk_summary"), (String) row.get("optimization_suggestion"),
                (String) row.get("analysis_model"), recommendations,
                ((java.sql.Timestamp) row.get("create_time")).toLocalDateTime());
    }

    private Long currentInputRevision(Long userId, LocalDate date) {
        return jdbc.query("SELECT revision FROM nutrition_daily_state WHERE user_id=? AND record_date=?", rs -> rs.next() ? rs.getLong(1) : 0L, userId, date);
    }
    private Long currentProfileRevision(Long userId) {
        return jdbc.queryForObject("SELECT COALESCE(MAX(profile_revision),0) FROM user_health_profile WHERE user_id=?", Long.class, userId);
    }
    private Map<String, Object> findByInput(Long userId, LocalDate date, Long input, Long profile, String source) {
        return one("SELECT * FROM health_analysis_task WHERE user_id=? AND task_type='DAILY_ANALYSIS' AND analysis_date=? AND input_revision=? AND profile_revision=? AND source_revision=? AND model_version=? ORDER BY id DESC LIMIT 1", userId, date, input, profile, source, MODEL_VERSION);
    }
    private Map<String, Object> findByRequest(String requestId) { return requestId == null ? null : one("SELECT * FROM health_analysis_task WHERE request_id=?", requestId); }
    private Map<String, Object> one(String sql, Object... args) { List<Map<String, Object>> rows = jdbc.queryForList(sql, args); return rows.isEmpty() ? null : rows.get(0); }
    private HealthAnalysisTaskResponse toTask(Map<String, Object> r) {
        return new HealthAnalysisTaskResponse(((Number) r.get("id")).longValue(), (String) r.get("request_id"), ((java.sql.Date) r.get("analysis_date")).toLocalDate(), ((Number) r.get("input_revision")).longValue(), ((Number) r.get("profile_revision")).longValue(), (String) r.get("source_revision"), (String) r.get("model_version"), (String) r.get("status"), ((Number) r.get("attempt_count")).intValue(), r.get("health_analysis_id") == null ? null : ((Number) r.get("health_analysis_id")).longValue(), (String) r.get("error_message"), ((java.sql.Timestamp) r.get("create_time")).toLocalDateTime(), ((java.sql.Timestamp) r.get("update_time")).toLocalDateTime());
    }
}
