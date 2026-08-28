package com.yizl.healthy.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_health_profile")
public class UserHealthProfileEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("height_cm")
    private BigDecimal heightCm;
    @TableField("weight_kg")
    private BigDecimal weightKg;
    @TableField("target_type")
    private String targetType;
    @TableField("target_weight_kg")
    private BigDecimal targetWeightKg;
    @TableField("target_duration_weeks")
    private Integer targetDurationWeeks;
    @TableField("activity_level")
    private String activityLevel;
    @TableField("diet_preferences")
    private String dietPreferences;
    @TableField("disliked_foods")
    private String dislikedFoods;
    private String allergies;
    @TableField("profile_revision")
    private Long profileRevision;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
