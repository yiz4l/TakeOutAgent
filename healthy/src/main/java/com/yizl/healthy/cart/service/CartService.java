package com.yizl.healthy.cart.service;

import com.yizl.healthy.cart.dto.request.AddCartItemRequest;
import com.yizl.healthy.cart.dto.request.UpdateCartItemRequest;
import com.yizl.healthy.cart.dto.response.CartItemResponse;
import com.yizl.healthy.cart.dto.response.CartMerchantGroupResponse;

import java.util.List;

public interface CartService {
    List<CartMerchantGroupResponse> getCart(Long userId, Long merchantId);

    CartItemResponse addItem(Long userId, AddCartItemRequest request);

    CartItemResponse updateItem(Long userId, Long cartItemId, UpdateCartItemRequest request);

    void deleteItem(Long userId, Long cartItemId);
}
