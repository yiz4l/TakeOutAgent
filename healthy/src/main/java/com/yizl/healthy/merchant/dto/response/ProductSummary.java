package com.yizl.healthy.merchant.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductSummary(
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
        List<String> nutritionTags
) {
}
