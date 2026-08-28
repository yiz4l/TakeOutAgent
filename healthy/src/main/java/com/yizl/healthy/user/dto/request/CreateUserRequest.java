package com.yizl.healthy.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 32, message = "姓名不能超过32个字符")
        String name,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须在8-64个字符之间")
        String password,

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @NotBlank(message = "性别不能为空")
        String gender,

        @Size(max = 500, message = "头像地址不能超过500个字符")
        String avatarPath
) {
}
