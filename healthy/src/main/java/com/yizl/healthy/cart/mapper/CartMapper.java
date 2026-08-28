package com.yizl.healthy.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yizl.healthy.cart.entity.CartEntity;
import com.yizl.healthy.cart.mapper.row.CartItemRow;
import com.yizl.healthy.cart.mapper.row.ProductSnapshotRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper extends BaseMapper<CartEntity> {
    int addOrIncrement(CartEntity item);

    List<CartItemRow> selectCartItems(@Param("userId") Long userId,
                                      @Param("merchantId") Long merchantId);

    CartItemRow selectCartItem(@Param("userId") Long userId,
                               @Param("cartItemId") Long cartItemId);

    int updateOwnedItem(@Param("userId") Long userId,
                        @Param("cartItemId") Long cartItemId,
                        @Param("quantity") Integer quantity,
                        @Param("selected") Integer selected);

    int deleteOwnedItem(@Param("userId") Long userId,
                        @Param("cartItemId") Long cartItemId);

    List<CartEntity> lockOwnedItems(@Param("userId") Long userId,
                                    @Param("cartItemIds") List<Long> cartItemIds);

    ProductSnapshotRow selectAvailableProduct(@Param("merchantId") Long merchantId,
                                              @Param("productType") String productType,
                                              @Param("productId") Long productId);

    int deleteCheckoutItemsAtOrBelowQuantity(@Param("userId") Long userId,
                                             @Param("orderId") Long orderId);

    int decrementCheckoutItemsAboveQuantity(@Param("userId") Long userId,
                                            @Param("orderId") Long orderId);
}
