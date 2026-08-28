package com.yizl.healthy.user.converter;

import com.yizl.healthy.user.dto.request.CreateUserRequest;
import com.yizl.healthy.user.dto.request.UpdateUserRequest;
import com.yizl.healthy.user.dto.response.UserProfileResponse;
import com.yizl.healthy.user.entity.UserEntity;

import java.time.LocalDateTime;

public final class UserConverter {

    private UserConverter() {
    }

    public static UserEntity toEntity(CreateUserRequest request, String passwordHash) {
        return UserEntity.builder()
                .name(request.name())
                .phone(request.phone())
                .gender(request.gender())
                .avatarPath(request.avatarPath())
                .passwordHash(passwordHash)
                .createTime(LocalDateTime.now())
                .role("USER")
                .status(1)
                .build();
    }

    public static void applyUpdate(UserEntity entity, UpdateUserRequest request) {
        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.gender() != null) {
            entity.setGender(request.gender());
        }
        if (request.avatarPath() != null) {
            entity.setAvatarPath(request.avatarPath());
        }
    }

    public static UserProfileResponse toResponse(UserEntity entity, Long merchantId) {
        return new UserProfileResponse(
                entity.getId(),
                entity.getName(),
                entity.getPhone(),
                entity.getGender(),
                entity.getAvatarPath(),
                entity.getRole(),
                merchantId
        );
    }
}
