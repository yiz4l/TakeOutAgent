package com.yizl.healthy.order.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.HashSet;
import java.util.List;

public record CreateOrderRequest(
        @NotNull @Positive Long merchantId,
        @NotNull @Positive Long addressId,
        @NotEmpty List<@NotNull @Positive Long> cartItemIds
) {
    @AssertTrue(message = "cartItemIds 不能包含重复项")
    public boolean isCartItemIdsUnique() {
        return cartItemIds == null || new HashSet<>(cartItemIds).size() == cartItemIds.size();
    }
}
