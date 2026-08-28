package com.yizl.healthy.merchant.mapper.row;

import lombok.Data;

@Data
public class MerchantSummaryRow {
    private Long id;
    private String name;
    private String avatarPath;
    private String description;
    private String address;
    private Integer businessStatus;
}
