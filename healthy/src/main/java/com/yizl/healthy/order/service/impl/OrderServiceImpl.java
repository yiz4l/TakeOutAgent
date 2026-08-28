package com.yizl.healthy.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yizl.healthy.address.entity.UserAddressEntity;
import com.yizl.healthy.address.mapper.AddressMapper;
import com.yizl.healthy.cart.entity.CartEntity;
import com.yizl.healthy.cart.mapper.CartMapper;
import com.yizl.healthy.cart.mapper.row.ProductSnapshotRow;
import com.yizl.healthy.common.api.PageResponse;
import com.yizl.healthy.common.exception.BusinessException;
import com.yizl.healthy.common.exception.ErrorCode;
import com.yizl.healthy.order.dto.request.CreateOrderRequest;
import com.yizl.healthy.order.dto.response.OrderDetailResponse;
import com.yizl.healthy.order.dto.response.OrderItemResponse;
import com.yizl.healthy.order.dto.response.OrderSummaryResponse;
import com.yizl.healthy.order.dto.response.PaymentResponse;
import com.yizl.healthy.order.dto.response.ReminderResponse;
import com.yizl.healthy.order.entity.OrderDetailEntity;
import com.yizl.healthy.order.entity.OrderEntity;
import com.yizl.healthy.order.mapper.OrderDetailMapper;
import com.yizl.healthy.order.mapper.OrderMapper;
import com.yizl.healthy.order.mapper.row.OrderViewRow;
import com.yizl.healthy.order.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private static final Set<String> ORDER_STATUSES = Set.of(
            "PENDING_PAYMENT", "PENDING_ACCEPT", "PREPARING", "DELIVERING", "COMPLETED", "CANCELLED");
    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final CartMapper cartMapper;
    private final AddressMapper addressMapper;

    public OrderServiceImpl(OrderMapper orderMapper, OrderDetailMapper orderDetailMapper,
                            CartMapper cartMapper, AddressMapper addressMapper) {
        this.orderMapper = orderMapper;
        this.orderDetailMapper = orderDetailMapper;
        this.cartMapper = cartMapper;
        this.addressMapper = addressMapper;
    }

    @Override
    @Transactional
    public OrderDetailResponse createOrder(Long userId, CreateOrderRequest request) {
        UserAddressEntity address = addressMapper.selectOwnedById(userId, request.addressId());
        if (address == null || address.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "收货地址不存在或不可用");
        }

        // 固定按主键顺序加锁，降低多个并发结算事务产生死锁的概率。
        List<CartEntity> cartItems = cartMapper.lockOwnedItems(userId, request.cartItemIds());
        if (cartItems.size() != request.cartItemIds().size()) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "部分购物车条目不存在或已被结算");
        }
        if (cartItems.stream().anyMatch(item -> !request.merchantId().equals(item.getMerchantId())
                || !Integer.valueOf(1).equals(item.getSelected()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "购物车条目必须属于指定商家且已勾选");
        }

        List<ProductSnapshotRow> products = cartItems.stream()
                .map(item -> requireAvailableProduct(request.merchantId(), item))
                .toList();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < cartItems.size(); i++) {
            total = total.add(products.get(i).getPrice().multiply(BigDecimal.valueOf(cartItems.get(i).getQuantity())));
        }

        OrderEntity order = OrderEntity.builder()
                .orderNo(newOrderNo())
                .userId(userId)
                .merchantId(request.merchantId())
                .status("PENDING_PAYMENT")
                .totalAmount(total)
                .paidAmount(total)
                .deliveryAddressSnapshot(address.getAddress())
                .recipientPhoneSnapshot(address.getContactPhone())
                .reminderCount(0)
                .build();
        orderMapper.insert(order);

        for (int i = 0; i < cartItems.size(); i++) {
            CartEntity cartItem = cartItems.get(i);
            ProductSnapshotRow product = products.get(i);
            orderDetailMapper.insert(OrderDetailEntity.builder()
                    .orderId(order.getId())
                    .productId(cartItem.getProductId())
                    .productType(cartItem.getProductType())
                    .productNameSnapshot(product.getName())
                    .productPriceSnapshot(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .build());
        }
        return requireOrder(userId, order.getId());
    }

    @Override
    @Transactional
    public PaymentResponse pay(Long userId, Long orderId) {
        OrderEntity order = lockOwnedOrder(userId, orderId);
        if ("PENDING_ACCEPT".equals(order.getStatus()) && order.getPaymentTime() != null) {
            return toPayment(order);
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "当前订单状态不允许支付");
        }
        if (orderMapper.markPaid(userId, orderId) != 1) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "订单状态已发生变化，请刷新后重试");
        }

        // 先删除数量不足或恰好相等的行，再扣减更大的行，保留下单后新增的同款数量。
        cartMapper.deleteCheckoutItemsAtOrBelowQuantity(userId, orderId);
        cartMapper.decrementCheckoutItemsAboveQuantity(userId, orderId);
        return toPayment(lockOwnedOrder(userId, orderId));
    }

    @Override
    @Transactional
    public OrderDetailResponse cancel(Long userId, Long orderId) {
        OrderEntity order = lockOwnedOrder(userId, orderId);
        if ("CANCELLED".equals(order.getStatus())) {
            return requireOrder(userId, orderId);
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus()) || orderMapper.cancelPendingOrder(userId, orderId) != 1) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "仅未支付订单可以取消");
        }
        return requireOrder(userId, orderId);
    }

    @Override
    public PageResponse<OrderSummaryResponse> getOrders(Long userId, String status, int page, int size) {
        if (status != null && !ORDER_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单状态不合法");
        }
        long total = orderMapper.countOwnedOrders(userId, status);
        if (total == 0) {
            return new PageResponse<>(0, page, size, List.of());
        }
        List<OrderSummaryResponse> records = orderMapper
                .selectOwnedOrderPage(userId, status, (long) (page - 1) * size, size)
                .stream().map(this::toSummary).toList();
        return new PageResponse<>(total, page, size, records);
    }

    @Override
    public OrderDetailResponse getOrder(Long userId, Long orderId) {
        return requireOrder(userId, orderId);
    }

    @Override
    @Transactional
    public ReminderResponse remind(Long userId, Long orderId) {
        OrderEntity before = lockOwnedOrder(userId, orderId);
        if (!"PENDING_ACCEPT".equals(before.getStatus()) && !"PREPARING".equals(before.getStatus())) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "当前订单状态不允许催单");
        }
        if (orderMapper.addReminderWithCooldown(userId, orderId) != 1) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "两次催单至少间隔 5 分钟");
        }
        OrderEntity after = lockOwnedOrder(userId, orderId);
        return new ReminderResponse(after.getId(), after.getReminderCount(), after.getLastReminderTime());
    }

    private ProductSnapshotRow requireAvailableProduct(Long merchantId, CartEntity item) {
        ProductSnapshotRow product = cartMapper.selectAvailableProduct(
                merchantId, item.getProductType(), item.getProductId());
        if (product == null) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "购物车中存在下架商品或商家已休息");
        }
        return product;
    }

    private OrderEntity lockOwnedOrder(Long userId, Long orderId) {
        OrderEntity order = orderMapper.selectOwnedForUpdate(userId, orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private OrderDetailResponse requireOrder(Long userId, Long orderId) {
        OrderViewRow order = orderMapper.selectOwnedView(userId, orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
        }
        List<OrderItemResponse> items = orderDetailMapper.selectList(
                        Wrappers.<OrderDetailEntity>lambdaQuery()
                                .eq(OrderDetailEntity::getOrderId, orderId)
                                .orderByAsc(OrderDetailEntity::getId))
                .stream().map(item -> new OrderItemResponse(item.getProductId(), item.getProductType(),
                        item.getProductNameSnapshot(), item.getProductPriceSnapshot(), item.getQuantity()))
                .toList();
        return new OrderDetailResponse(order.getId(), order.getOrderNo(), order.getMerchantId(),
                order.getMerchantName(), order.getStatus(), order.getOrderTime(), order.getPaymentTime(),
                order.getTotalAmount(), order.getPaidAmount(), order.getDeliveryAddress(),
                order.getRecipientPhone(), order.getReminderCount(), items);
    }

    private OrderSummaryResponse toSummary(OrderViewRow row) {
        return new OrderSummaryResponse(row.getId(), row.getOrderNo(), row.getMerchantId(),
                row.getMerchantName(), row.getStatus(), row.getOrderTime(), row.getTotalAmount(),
                row.getPaidAmount(), row.getItemCount());
    }

    private PaymentResponse toPayment(OrderEntity order) {
        return new PaymentResponse(order.getId(), order.getStatus(), order.getPaymentTime(), order.getPaidAmount());
    }

    private String newOrderNo() {
        return LocalDateTime.now().format(ORDER_NO_TIME)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
