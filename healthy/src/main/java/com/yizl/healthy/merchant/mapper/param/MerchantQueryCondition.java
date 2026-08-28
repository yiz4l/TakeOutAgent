package com.yizl.healthy.merchant.mapper.param;

public record MerchantQueryCondition(
        String keyword,
        Long categoryId,
        String productType,
        long offset,
        int size
) {
}
