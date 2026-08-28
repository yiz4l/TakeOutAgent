package com.yizl.healthy.order.service;

import com.yizl.healthy.common.api.PageResponse;
import com.yizl.healthy.order.dto.request.CreateOrderRequest;
import com.yizl.healthy.order.dto.response.OrderDetailResponse;
import com.yizl.healthy.order.dto.response.OrderSummaryResponse;
import com.yizl.healthy.order.dto.response.PaymentResponse;
import com.yizl.healthy.order.dto.response.ReminderResponse;

public interface OrderService {
    OrderDetailResponse createOrder(Long userId, CreateOrderRequest request);
    PaymentResponse pay(Long userId, Long orderId);
    OrderDetailResponse cancel(Long userId, Long orderId);
    PageResponse<OrderSummaryResponse> getOrders(Long userId, String status, int page, int size);
    OrderDetailResponse getOrder(Long userId, Long orderId);
    ReminderResponse remind(Long userId, Long orderId);
}
