package com.yizl.healthy.merchant.mapper.row;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSummaryRow {
    private Long id;
    private String productType;
    private Long merchantId;
    private String merchantName;
    private Long categoryId;
    private String categoryName;
    private String name;
    private BigDecimal price;
    private String imagePath;
    private String description;
    private Integer salesCount;
    private String nutritionTagsJson;
}
