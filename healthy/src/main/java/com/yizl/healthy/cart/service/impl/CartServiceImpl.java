package com.yizl.healthy.cart.service.impl;

import com.yizl.healthy.cart.dto.request.AddCartItemRequest;
import com.yizl.healthy.cart.dto.request.UpdateCartItemRequest;
import com.yizl.healthy.cart.dto.response.CartItemResponse;
import com.yizl.healthy.cart.dto.response.CartMerchantGroupResponse;
import com.yizl.healthy.cart.entity.CartEntity;
import com.yizl.healthy.cart.mapper.CartMapper;
import com.yizl.healthy.cart.mapper.row.CartItemRow;
import com.yizl.healthy.cart.mapper.row.ProductSnapshotRow;
import com.yizl.healthy.cart.service.CartService;
import com.yizl.healthy.common.exception.BusinessException;
import com.yizl.healthy.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {
    private final CartMapper cartMapper;

    public CartServiceImpl(CartMapper cartMapper) {
        this.cartMapper = cartMapper;
    }

    @Override
    public List<CartMerchantGroupResponse> getCart(Long userId, Long merchantId) {
        Map<Long, MutableGroup> groups = new LinkedHashMap<>();
        for (CartItemRow row : cartMapper.selectCartItems(userId, merchantId)) {
            MutableGroup group = groups.computeIfAbsent(row.getMerchantId(),
                    ignored -> new MutableGroup(row.getMerchantId(), row.getMerchantName()));
            group.items.add(toResponse(row));
        }
        return groups.values().stream()
                .map(group -> new CartMerchantGroupResponse(group.merchantId, group.merchantName, group.items))
                .toList();
    }

    @Override
    @Transactional
    public CartItemResponse addItem(Long userId, AddCartItemRequest request) {
        ProductSnapshotRow product = cartMapper.selectAvailableProduct(
                request.merchantId(), request.productType(), request.productId());
        if (product == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商品不存在、未在售或不属于该商家");
        }

        CartEntity item = CartEntity.builder()
                .userId(userId)
                .merchantId(request.merchantId())
                .productType(request.productType())
                .productId(request.productId())
                .quantity(request.quantity())
                .selected(Boolean.TRUE.equals(request.selected()) ? 1 : 0)
                .build();
        cartMapper.addOrIncrement(item);
        return requireItem(userId, item.getId());
    }

    @Override
    @Transactional
    public CartItemResponse updateItem(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        Integer selected = request.selected() == null ? null : (request.selected() ? 1 : 0);
        if (cartMapper.updateOwnedItem(userId, cartItemId, request.quantity(), selected) == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "购物车条目不存在");
        }
        return requireItem(userId, cartItemId);
    }

    @Override
    @Transactional
    public void deleteItem(Long userId, Long cartItemId) {
        if (cartMapper.deleteOwnedItem(userId, cartItemId) == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "购物车条目不存在");
        }
    }

    private CartItemResponse requireItem(Long userId, Long cartItemId) {
        CartItemRow row = cartMapper.selectCartItem(userId, cartItemId);
        if (row == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "购物车条目不存在");
        }
        return toResponse(row);
    }

    private CartItemResponse toResponse(CartItemRow row) {
        return new CartItemResponse(row.getId(), row.getProductType(), row.getProductId(),
                row.getName(), row.getImagePath(), row.getUnitPrice(), row.getQuantity(),
                Integer.valueOf(1).equals(row.getSelected()));
    }

    private static final class MutableGroup {
        private final Long merchantId;
        private final String merchantName;
        private final List<CartItemResponse> items = new ArrayList<>();

        private MutableGroup(Long merchantId, String merchantName) {
            this.merchantId = merchantId;
            this.merchantName = merchantName;
        }
    }
}
