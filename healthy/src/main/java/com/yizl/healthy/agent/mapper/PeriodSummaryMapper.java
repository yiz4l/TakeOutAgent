package com.yizl.healthy.agent.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface PeriodSummaryMapper {
    @Insert("INSERT INTO health_period_summary(user_id, period_type, period_start, period_end, source_revision, profile_revision, summary, nutrient_trends, model_version) VALUES(#{userId}, #{periodType}, #{periodStart}, #{periodEnd}, #{sourceRevision}, #{profileRevision}, #{summary}, #{nutrientTrends}, #{modelVersion}) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id), summary=VALUES(summary), nutrient_trends=VALUES(nutrient_trends), update_time=CURRENT_TIMESTAMP")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsert(@Param("id") Long id, @Param("userId") Long userId, @Param("periodType") String periodType,
               @Param("periodStart") LocalDate periodStart, @Param("periodEnd") LocalDate periodEnd,
               @Param("sourceRevision") String sourceRevision, @Param("profileRevision") Long profileRevision,
               @Param("summary") String summary, @Param("nutrientTrends") String nutrientTrends,
               @Param("modelVersion") String modelVersion);
}
