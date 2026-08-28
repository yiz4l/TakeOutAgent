package com.yizl.healthy.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yizl.healthy.common.exception.BusinessException;
import com.yizl.healthy.common.exception.ErrorCode;
import com.yizl.healthy.common.security.JwtService;
import com.yizl.healthy.merchant.mapper.MerchantMapper;
import com.yizl.healthy.user.converter.UserConverter;
import com.yizl.healthy.user.dto.request.LoginRequest;
import com.yizl.healthy.user.dto.response.LoginResponse;
import com.yizl.healthy.user.dto.response.UserProfileResponse;
import com.yizl.healthy.user.entity.UserEntity;
import com.yizl.healthy.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserMapper userMapper,
            MerchantMapper merchantMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userMapper.selectOne(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getPhone, request.phone())
        );

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "手机号或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.AUTH_ERROR, "账号已被禁用");
        }

        Long merchantId = null;
        if ("MERCHANT".equals(user.getRole())) {
            merchantId = merchantMapper.selectIdByUserId(user.getId());
        }

        UserProfileResponse profile = UserConverter.toResponse(user, merchantId);
        String accessToken = jwtService.createToken(user.getId(), user.getRole());
        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                profile
        );
    }
}
