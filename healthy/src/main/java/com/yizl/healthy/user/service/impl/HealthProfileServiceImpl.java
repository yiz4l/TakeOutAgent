package com.yizl.healthy.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizl.healthy.common.exception.BusinessException;
import com.yizl.healthy.common.exception.ErrorCode;
import com.yizl.healthy.user.dto.request.UpdateHealthProfileRequest;
import com.yizl.healthy.user.dto.response.HealthProfileResponse;
import com.yizl.healthy.user.entity.UserHealthProfileEntity;
import com.yizl.healthy.user.mapper.UserHealthProfileMapper;
import com.yizl.healthy.user.service.HealthProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HealthProfileServiceImpl implements HealthProfileService {
    private final UserHealthProfileMapper mapper;
    private final ObjectMapper objectMapper;

    public HealthProfileServiceImpl(UserHealthProfileMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public HealthProfileResponse get(Long userId) {
        UserHealthProfileEntity entity = find(userId);
        return entity == null ? null : toResponse(entity);
    }

    @Override
    @Transactional
    public HealthProfileResponse update(Long userId, UpdateHealthProfileRequest request) {
        UserHealthProfileEntity entity = find(userId);
        LocalDateTime now = LocalDateTime.now();
        if (entity == null) {
            entity = new UserHealthProfileEntity();
            entity.setUserId(userId);
            entity.setProfileRevision(1L);
            entity.setCreateTime(now);
        } else {
            entity.setProfileRevision(entity.getProfileRevision() + 1);
        }
        entity.setHeightCm(request.heightCm());
        entity.setWeightKg(request.weightKg());
        entity.setTargetType(request.targetType());
        entity.setTargetWeightKg(request.targetWeightKg());
        entity.setTargetDurationWeeks(request.targetDurationWeeks());
        entity.setActivityLevel(request.activityLevel());
        entity.setDietPreferences(writeJson(request.dietPreferences()));
        entity.setDislikedFoods(writeJson(request.dislikedFoods()));
        entity.setAllergies(writeJson(request.allergies()));
        entity.setUpdateTime(now);
        if (entity.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return toResponse(entity);
    }

    private UserHealthProfileEntity find(Long userId) {
        return mapper.selectOne(new LambdaQueryWrapper<UserHealthProfileEntity>()
                .eq(UserHealthProfileEntity::getUserId, userId));
    }

    private HealthProfileResponse toResponse(UserHealthProfileEntity entity) {
        return new HealthProfileResponse(entity.getUserId().toString(), entity.getHeightCm(),
                entity.getWeightKg(), entity.getTargetType(), entity.getTargetWeightKg(),
                entity.getTargetDurationWeeks(), entity.getActivityLevel(),
                readJson(entity.getDietPreferences()), readJson(entity.getDislikedFoods()),
                readJson(entity.getAllergies()), entity.getProfileRevision().toString());
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "健康画像列表格式错误");
        }
    }

    private List<String> readJson(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "健康画像数据无法解析");
        }
    }
}
