package com.yizl.healthy.address.converter;

import com.yizl.healthy.address.dto.request.AddressRequest;
import com.yizl.healthy.address.dto.response.AddressResponse;
import com.yizl.healthy.address.entity.UserAddressEntity;

public final class AddressConverter {

    private AddressConverter() {
    }

    public static UserAddressEntity toEntity(Long userId, AddressRequest request) {
        return UserAddressEntity.builder()
                .userId(userId)
                .address(request.address())
                .contactPhone(request.contactPhone())
                .enabled(Boolean.TRUE.equals(request.enabled()) ? 1 : 0)
                .remark(request.remark())
                .build();
    }

    public static void applyUpdate(UserAddressEntity entity, AddressRequest request) {
        if (request.address() != null) {
            entity.setAddress(request.address());
        }
        if (request.contactPhone() != null) {
            entity.setContactPhone(request.contactPhone());
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled() ? 1 : 0);
        }
        if (request.remark() != null) {
            entity.setRemark(request.remark());
        }
    }

    public static AddressResponse toResponse(UserAddressEntity entity) {
        return new AddressResponse(
                entity.getId(),
                entity.getAddress(),
                entity.getContactPhone(),
                entity.getEnabled() == 1,
                entity.getRemark()
        );
    }
}
