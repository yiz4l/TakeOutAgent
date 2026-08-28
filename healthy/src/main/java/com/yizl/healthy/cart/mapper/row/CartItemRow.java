package com.yizl.healthy.cart.mapper.row;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemRow {
    private Long id;
    private Long merchantId;
    private String merchantName;
    private String productType;
    private Long productId;
    private String name;
    private String imagePath;
    private BigDecimal unitPrice;
    private Integer quantity;
    private Integer selected;
}
