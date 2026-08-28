package com.yizl.healthy.cart.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(1) Integer quantity,
        Boolean selected
) {
    @AssertTrue(message = "quantity 和 selected 至少提供一个")
    public boolean isAnyFieldPresent() {
        return quantity != null || selected != null;
    }
}
