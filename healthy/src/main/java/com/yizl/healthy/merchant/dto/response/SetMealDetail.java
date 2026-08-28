package com.yizl.healthy.merchant.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SetMealDetail(
        Long id,
        String productType,
        Long merchantId,
        String merchantName,
        String name,
        BigDecimal price,
        String imagePath,
        String description,
        int salesCount,
        List<SetMealDish> dishes
) {
}
