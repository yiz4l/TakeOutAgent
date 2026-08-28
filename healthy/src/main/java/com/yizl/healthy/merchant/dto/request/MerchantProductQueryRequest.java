package com.yizl.healthy.merchant.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MerchantProductQueryRequest(
        @Size(max = 100, message = "搜索关键词不能超过100个字符")
        String keyword,

        @Positive(message = "分类ID必须为正数")
        Long categoryId,

        @Pattern(regexp = "DISH|SETMEAL", message = "商品类型必须为DISH或SETMEAL")
        String productType,

        @Min(value = 1, message = "页码不能小于1")
        Integer page,

        @Min(value = 1, message = "每页数量不能小于1")
        @Max(value = 100, message = "每页数量不能超过100")
        Integer size
) {
    public MerchantProductQueryRequest {
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }
}
