package com.yizl.healthy.agent.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class PeriodSourceRevisionService {
    private final JdbcTemplate jdbc;

    public PeriodSourceRevisionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String calculate(Long userId, String taskType, LocalDate periodEnd) {
        List<Map<String, Object>> rows;
        if ("MONTHLY_SUMMARY".equals(taskType)) {
            rows = jdbc.queryForList("SELECT id, source_revision FROM health_period_summary WHERE user_id=? AND period_type='BIWEEKLY' AND period_end<=? ORDER BY period_end DESC LIMIT 2", userId, periodEnd);
        } else {
            rows = jdbc.queryForList("SELECT record_date, revision FROM nutrition_daily_state WHERE user_id=? AND record_date BETWEEN DATE_SUB(?, INTERVAL 13 DAY) AND ? ORDER BY record_date", userId, periodEnd, periodEnd);
        }
        String material = rows.toString();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("cannot calculate period source revision", exception);
        }
    }
}
