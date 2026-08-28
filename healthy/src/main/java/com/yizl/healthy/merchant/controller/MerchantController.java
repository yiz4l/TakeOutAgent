package com.yizl.healthy.merchant.controller;

import com.yizl.healthy.common.api.ApiResponse;
import com.yizl.healthy.common.api.PageResponse;
import com.yizl.healthy.merchant.dto.request.MerchantListQueryRequest;
import com.yizl.healthy.merchant.dto.request.MerchantProductQueryRequest;
import com.yizl.healthy.merchant.dto.request.ProductQueryRequest;
import com.yizl.healthy.merchant.dto.response.*;
import com.yizl.healthy.merchant.mapper.param.ProductQueryCondition;
import com.yizl.healthy.merchant.service.MerchantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
public class MerchantController {
    private final MerchantService merchantService;
    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/merchants")
    public PageResponse<MerchantSummary> getMerchants(
            @Valid MerchantListQueryRequest merchantListQueryRequest
    ){
        return merchantService.getMerchants(merchantListQueryRequest);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/merchants/{merchantId}")
    public ApiResponse<MerchantDetail> getMerchantDetail(
            @Positive
            @PathVariable("merchantId") Long merchantId
    ){
        return ApiResponse.success(merchantService.getMerchantDetail(merchantId));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/merchants/{merchantId}/products")
    public PageResponse<ProductSummary> getMerchantProducts(
            @Positive @PathVariable("merchantId") Long merchantId,
            @Valid MerchantProductQueryRequest merchantProductQueryRequest
            ){
        return merchantService.getMerchantProducts(merchantId, merchantProductQueryRequest);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/dishCategories")
    public ApiResponse<List<DishCategories>> getDishCategories(){
        return ApiResponse.success(merchantService.getDishCategories());
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/products")
    public PageResponse<ProductSummary> getProducts(
            @Valid ProductQueryRequest productQueryRequest
    ){
        return merchantService.getProducts(productQueryRequest);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/dishes/{dishId}")
    public ApiResponse<DishDetail> getDishDetail(
            @PathVariable Long dishId
    ){
        return ApiResponse.success(merchantService.getDishDetail(dishId));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/setmeals/{setmealId}")
    public ApiResponse<SetMealDetail> getSetMealDetail(
            @PathVariable Long setmealId
    ){
        return ApiResponse.success(merchantService.getSetMealDetail(setmealId));
    }

}
