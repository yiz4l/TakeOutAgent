package com.yizl.healthy.merchant.dto.response;

import java.util.List;

public record DishCategories(
        Long id,
        String name,
        int initialSort,
        List<String> nutritionTags
) {
}
