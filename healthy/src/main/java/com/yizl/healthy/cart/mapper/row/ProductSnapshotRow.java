package com.yizl.healthy.cart.mapper.row;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSnapshotRow {
    private Long productId;
    private String productType;
    private Long merchantId;
    private String name;
    private String imagePath;
    private BigDecimal price;
}
