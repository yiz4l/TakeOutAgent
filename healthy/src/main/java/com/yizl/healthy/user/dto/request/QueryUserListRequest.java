package com.yizl.healthy.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QueryUserListRequest(
        @Size(max = 32, message = "姓名不能超过32个字符")
        String name,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        @Size(max = 2, message = "性别不能超过2个字符")
        String gender,

        @Min(value = 1, message = "页码不能小于1")
        Integer page,

        @Min(value = 1, message = "每页数量不能小于1")
        @Max(value = 100, message = "每页数量不能超过100")
        Integer size
) {
    public QueryUserListRequest {
        page = page == 0 ? 1 : page;
        size = size == 0 ? 20 : size;
    }
}
