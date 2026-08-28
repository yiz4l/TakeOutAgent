package com.yizl.healthy.merchant.dto.response;

import java.util.List;

public record MerchantSummary(
        Long id,
        String name,
        String avatarPath,
        String description,
        String address,
        int businessStatus
) {
}
