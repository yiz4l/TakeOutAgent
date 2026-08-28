package com.yizl.healthy.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Login phone number can't be empty")
        String phone,

        @NotBlank(message = "password can't be empty")
        String password
) {
}
