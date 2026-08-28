package com.yizl.healthy.cart.dto.response;

import java.util.List;

public record CartMerchantGroupResponse(
        Long merchantId,
        String merchantName,
        List<CartItemResponse> items
) {
    public CartMerchantGroupResponse {
        items = List.copyOf(items);
    }
}
