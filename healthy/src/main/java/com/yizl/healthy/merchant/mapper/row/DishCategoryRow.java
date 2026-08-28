package com.yizl.healthy.merchant.mapper.row;

import lombok.Data;

@Data
public class DishCategoryRow {
    private Long id;
    private String name;
    private Integer initialSort;
    private String nutritionTagsJson;
}
