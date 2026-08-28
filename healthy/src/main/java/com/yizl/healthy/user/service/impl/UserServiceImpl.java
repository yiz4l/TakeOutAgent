package com.yizl.healthy.user.service.impl;

import com.yizl.healthy.common.exception.BusinessException;
import com.yizl.healthy.common.exception.ErrorCode;
import com.yizl.healthy.merchant.mapper.MerchantMapper;
import com.yizl.healthy.user.converter.UserConverter;
import com.yizl.healthy.user.dto.request.CreateUserRequest;
import com.yizl.healthy.user.dto.request.UpdateUserRequest;
import com.yizl.healthy.user.dto.response.UserProfileResponse;
import com.yizl.healthy.user.entity.UserEntity;
import com.yizl.healthy.user.mapper.UserMapper;
import com.yizl.healthy.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserMapper userMapper,
            MerchantMapper merchantMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    @Transactional
    public UserProfileResponse register(CreateUserRequest request) {
        ensurePhoneAvailable(request.phone(), null);
        String passwordHash = passwordEncoder.encode(request.password());
        UserEntity entity = UserConverter.toEntity(request, passwordHash);
        entity.setCreateTime(LocalDateTime.now());
        userMapper.insert(entity);
        return UserConverter.toResponse(entity, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(Long userId) {
        UserEntity userEntity = requireUser(userId);
        return buildProfile(userEntity);
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentUser(Long userId, UpdateUserRequest request) {
        UserEntity entity = requireUser(userId);
        UserConverter.applyUpdate(entity, request);
        userMapper.updateById(entity);
        return buildProfile(entity);
    }

    private UserEntity requireUser(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.AUTH_ERROR);
        }
        return entity;
    }

    private UserProfileResponse buildProfile(UserEntity userEntity){
        Long merchantId = null;
        if ("MERCHANT".equals(userEntity.getRole())){
            merchantId = merchantMapper.selectIdByUserId(userEntity.getId());
        }
        return UserConverter.toResponse(userEntity, merchantId);
    }

    private void ensurePhoneAvailable(String phone, Long excludedUserId) {
        if (userMapper.countByPhoneExcludingId(phone, excludedUserId) > 0) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
    }

}
