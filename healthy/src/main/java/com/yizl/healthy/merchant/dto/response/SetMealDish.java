package com.yizl.healthy.merchant.dto.response;

import java.math.BigDecimal;

public record SetMealDish(
        Long dishId,
        String name,
        String imagePath,
        BigDecimal copies
) {
}
