package com.yizl.healthy.user.service;

import com.yizl.healthy.user.dto.request.CreateUserRequest;
import com.yizl.healthy.user.dto.request.UpdateUserRequest;
import com.yizl.healthy.user.dto.response.UserProfileResponse;

public interface UserService {

    UserProfileResponse register(CreateUserRequest request);

    UserProfileResponse getCurrentUser(Long userId);
    UserProfileResponse updateCurrentUser(Long userId, UpdateUserRequest request);
}
