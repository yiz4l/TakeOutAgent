package com.yizl.healthy.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        Long id,
        String orderNo,
        Long merchantId,
        String merchantName,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime orderTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime paymentTime,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        String deliveryAddress,
        String recipientPhone,
        Integer reminderCount,
        List<OrderItemResponse> items
) {
    public OrderDetailResponse {
        items = List.copyOf(items);
    }
}
