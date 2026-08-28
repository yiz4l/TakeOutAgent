package com.yizl.healthy.user.controller;

import com.yizl.healthy.common.api.ApiResponse;
import com.yizl.healthy.common.security.AuthenticatedUser;
import com.yizl.healthy.user.dto.request.CreateUserRequest;
import com.yizl.healthy.user.dto.request.LoginRequest;
import com.yizl.healthy.user.dto.request.UpdateUserRequest;
import com.yizl.healthy.user.dto.response.LoginResponse;
import com.yizl.healthy.user.dto.response.UserProfileResponse;
import com.yizl.healthy.user.dto.request.UpdateHealthProfileRequest;
import com.yizl.healthy.user.dto.response.HealthProfileResponse;
import com.yizl.healthy.user.service.AuthService;
import com.yizl.healthy.user.service.HealthProfileService;
import com.yizl.healthy.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final HealthProfileService healthProfileService;

    public UserController(UserService userService, AuthService authService, HealthProfileService healthProfileService) {
        this.userService = userService;
        this.authService = authService;
        this.healthProfileService = healthProfileService;
    }

    @GetMapping("/user/health-profile")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<HealthProfileResponse> getHealthProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(healthProfileService.get(currentUser.userId()));
    }

    @PutMapping("/user/health-profile")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<HealthProfileResponse> updateHealthProfile(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateHealthProfileRequest request
    ) {
        return ApiResponse.success(healthProfileService.update(currentUser.userId(), request));
    }

    @PostMapping("/user/register")
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    @PostMapping("/user/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/user/me")
    public ApiResponse<UserProfileResponse> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(userService.getCurrentUser(currentUser.userId()));
    }

    @PutMapping("/users/update")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<UserProfileResponse> updateCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success(
                userService.updateCurrentUser(currentUser.userId(), request)
        );
    }

}
