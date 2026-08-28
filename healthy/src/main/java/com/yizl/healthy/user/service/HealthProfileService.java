package com.yizl.healthy.user.service;

import com.yizl.healthy.user.dto.request.UpdateHealthProfileRequest;
import com.yizl.healthy.user.dto.response.HealthProfileResponse;

public interface HealthProfileService {
    HealthProfileResponse get(Long userId);
    HealthProfileResponse update(Long userId, UpdateHealthProfileRequest request);
}
