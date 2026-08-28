package com.yizl.healthy.merchant.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DishDetail(
        Long id,
        String productType,
        Long merchantId,
        String merchantName,
        Long categoryId,
        String categoryName,
        String name,
        BigDecimal price,
        String imagePath,
        String description,
        int salesCount,
        List<String> nutritionTags,
        NutritionDetail nutritionDetail
) {
}
