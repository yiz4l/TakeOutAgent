package com.yizl.healthy.order.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productType,
        String productName,
        BigDecimal productPrice,
        Integer quantity
) {
}
