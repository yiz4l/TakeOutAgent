package com.yizl.healthy.merchant.service;

import com.yizl.healthy.common.api.PageResponse;
import com.yizl.healthy.merchant.dto.request.MerchantListQueryRequest;
import com.yizl.healthy.merchant.dto.request.MerchantProductQueryRequest;
import com.yizl.healthy.merchant.dto.request.ProductQueryRequest;
import com.yizl.healthy.merchant.dto.response.*;

import java.util.List;

public interface MerchantService {
    PageResponse<MerchantSummary> getMerchants(MerchantListQueryRequest request);

    MerchantDetail getMerchantDetail(Long merchantId);

    PageResponse<ProductSummary> getMerchantProducts(
            Long merchantId,
            MerchantProductQueryRequest request
    );

    List<DishCategories> getDishCategories();

    PageResponse<ProductSummary> getProducts(ProductQueryRequest request);

    DishDetail getDishDetail(Long dishId);

    SetMealDetail getSetMealDetail(Long setMealId);
}
