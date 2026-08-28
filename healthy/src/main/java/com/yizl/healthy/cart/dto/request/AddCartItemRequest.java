package com.yizl.healthy.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull @Positive Long merchantId,
        @NotNull @Pattern(regexp = "DISH|SETMEAL") String productType,
        @NotNull @Positive Long productId,
        @NotNull @Min(1) Integer quantity,
        @NotNull Boolean selected
) {
}
