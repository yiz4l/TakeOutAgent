package com.yizl.healthy.address.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "地址不能为空", groups = Create.class)
        @Size(max = 255, message = "用户地址不能超过255个字符")
        String address,
        @NotBlank(message = "手机号不能为空", groups = Create.class)
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String contactPhone,
        @NotNull(message = "是否启用不能为空", groups = Create.class)
        Boolean enabled,
        @Size(max = 255, message = "用户备注不能超过255个字符")
        String remark
) {
    public interface Create {
    }
}
