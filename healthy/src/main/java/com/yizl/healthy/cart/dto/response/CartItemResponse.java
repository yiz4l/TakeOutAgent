package com.yizl.healthy.cart.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        String productType,
        Long productId,
        String name,
        String imagePath,
        BigDecimal unitPrice,
        Integer quantity,
        Boolean selected
) {
}
