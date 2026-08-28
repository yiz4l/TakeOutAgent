package com.yizl.healthy.merchant.dto.response;

import java.util.List;

public record MerchantDetail(
        Long id,
        String name,
        String avatarPath,
        String description,
        String address,
        String phone,
        String businessHours,
        int businessStatus,
        List<ProductSummary> products
) {
    public MerchantDetail{
        products = products == null ? List.of() : List.copyOf(products);
    }
}
