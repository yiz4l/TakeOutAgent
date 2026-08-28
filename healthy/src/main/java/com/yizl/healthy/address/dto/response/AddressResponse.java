package com.yizl.healthy.address.dto.response;

public record AddressResponse(
        Long id,
        String address,
        String contactPhone,
        Boolean enabled,
        String remark
) {
}
