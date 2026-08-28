package com.yizl.healthy.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        String name,
        String phone,
        String gender,
        String avatarPath,
        String role,
        @JsonSerialize(using = ToStringSerializer.class)
        Long merchantId
) {
}
