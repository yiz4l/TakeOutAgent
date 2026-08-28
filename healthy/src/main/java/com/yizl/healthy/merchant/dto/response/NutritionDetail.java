package com.yizl.healthy.merchant.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record NutritionDetail(
        BigDecimal calories,
        @JsonProperty("protein_g")
        BigDecimal proteinG,
        @JsonProperty("fat_g")
        BigDecimal fatG,
        @JsonProperty("carbohydrate_g")
        BigDecimal carbohydrateG
) {
}
