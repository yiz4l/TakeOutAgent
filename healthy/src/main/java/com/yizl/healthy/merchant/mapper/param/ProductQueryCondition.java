package com.yizl.healthy.merchant.mapper.param;

public record ProductQueryCondition(
        Long merchantId,
        Long categoryId,
        String keyword,
        String productType,
        long offset,
        int size
) {
}
