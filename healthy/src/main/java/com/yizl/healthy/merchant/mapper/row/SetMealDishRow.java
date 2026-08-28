package com.yizl.healthy.merchant.mapper.row;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SetMealDishRow {
    private Long dishId;
    private String name;
    private String imagePath;
    private BigDecimal copies;
}
