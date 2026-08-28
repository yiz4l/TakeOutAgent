package com.yizl.healthy.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yizl.healthy.order.entity.OrderEntity;
import com.yizl.healthy.order.mapper.row.OrderViewRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
    OrderEntity selectOwnedForUpdate(@Param("userId") Long userId, @Param("orderId") Long orderId);

    OrderViewRow selectOwnedView(@Param("userId") Long userId, @Param("orderId") Long orderId);

    long countOwnedOrders(@Param("userId") Long userId, @Param("status") String status);

    List<OrderViewRow> selectOwnedOrderPage(@Param("userId") Long userId,
                                            @Param("status") String status,
                                            @Param("offset") long offset,
                                            @Param("size") int size);

    int markPaid(@Param("userId") Long userId, @Param("orderId") Long orderId);

    int cancelPendingOrder(@Param("userId") Long userId, @Param("orderId") Long orderId);

    int addReminderWithCooldown(@Param("userId") Long userId, @Param("orderId") Long orderId);
}
