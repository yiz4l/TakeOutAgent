package com.yizl.healthy.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 1, max = 32, message = "姓名长度必须在1到32个字符之间")
        String name,

        String gender,

        @Size(max = 500, message = "头像地址不能超过500个字符")
        String avatarPath
) {
    @AssertTrue(message = "至少需要提供一个待修改字段")
    public boolean isAnyFieldPresent() {
        return name != null || gender != null || avatarPath != null;
    }
}
