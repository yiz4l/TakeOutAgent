package com.yizl.healthy.order.mapper.row;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderViewRow {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private String merchantName;
    private String status;
    private LocalDateTime orderTime;
    private LocalDateTime paymentTime;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String deliveryAddress;
    private String recipientPhone;
    private Integer reminderCount;
    private LocalDateTime lastReminderTime;
    private Integer itemCount;
}
