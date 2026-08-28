package com.yizl.healthy.order.controller;

import com.yizl.healthy.common.api.ApiResponse;
import com.yizl.healthy.common.api.PageResponse;
import com.yizl.healthy.common.security.AuthenticatedUser;
import com.yizl.healthy.order.dto.request.CreateOrderRequest;
import com.yizl.healthy.order.dto.response.OrderDetailResponse;
import com.yizl.healthy.order.dto.response.OrderSummaryResponse;
import com.yizl.healthy.order.dto.response.PaymentResponse;
import com.yizl.healthy.order.dto.response.ReminderResponse;
import com.yizl.healthy.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('USER')")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderDetailResponse> createOrder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrder(user.userId(), request));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<PaymentResponse> pay(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Positive @PathVariable Long orderId) {
        return ApiResponse.success(orderService.pay(user.userId(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderDetailResponse> cancel(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @Positive @PathVariable Long orderId) {
        return ApiResponse.success(orderService.cancel(user.userId(), orderId));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> getOrders(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String status,
            @Min(1) @RequestParam(defaultValue = "1") int page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(orderService.getOrders(user.userId(), status, page, size));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getOrder(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @Positive @PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrder(user.userId(), orderId));
    }

    @PostMapping("/{orderId}/reminders")
    public ApiResponse<ReminderResponse> remind(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Positive @PathVariable Long orderId) {
        return ApiResponse.success(orderService.remind(user.userId(), orderId));
    }
}
