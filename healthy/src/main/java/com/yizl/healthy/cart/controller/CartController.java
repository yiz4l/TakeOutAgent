package com.yizl.healthy.cart.controller;

import com.yizl.healthy.cart.dto.request.AddCartItemRequest;
import com.yizl.healthy.cart.dto.request.UpdateCartItemRequest;
import com.yizl.healthy.cart.dto.response.CartItemResponse;
import com.yizl.healthy.cart.dto.response.CartMerchantGroupResponse;
import com.yizl.healthy.cart.service.CartService;
import com.yizl.healthy.common.api.ApiResponse;
import com.yizl.healthy.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/cart-items")
@PreAuthorize("hasRole('USER')")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<List<CartMerchantGroupResponse>> getCart(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Positive @RequestParam(required = false) Long merchantId) {
        return ApiResponse.success(cartService.getCart(user.userId(), merchantId));
    }

    @PostMapping
    public ApiResponse<CartItemResponse> addItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cartService.addItem(user.userId(), request));
    }

    @PutMapping("/{cartItemId}")
    public ApiResponse<CartItemResponse> updateItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Positive @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(cartService.updateItem(user.userId(), cartItemId, request));
    }

    @DeleteMapping("/{cartItemId}")
    public ApiResponse<Void> deleteItem(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Positive @PathVariable Long cartItemId) {
        cartService.deleteItem(user.userId(), cartItemId);
        return ApiResponse.success(null);
    }
}
