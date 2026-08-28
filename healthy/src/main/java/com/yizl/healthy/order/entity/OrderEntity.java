package com.yizl.healthy.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("`orders`")
public class OrderEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_no")
    private String orderNo;
    @TableField("user_id")
    private Long userId;
    @TableField("merchant_id")
    private Long merchantId;
    private String status;
    @TableField("order_time")
    private LocalDateTime orderTime;
    @TableField("payment_time")
    private LocalDateTime paymentTime;
    @TableField("total_amount")
    private BigDecimal totalAmount;
    @TableField("paid_amount")
    private BigDecimal paidAmount;
    @TableField("delivery_address_snapshot")
    private String deliveryAddressSnapshot;
    @TableField("recipient_phone_snapshot")
    private String recipientPhoneSnapshot;
    @TableField("reminder_count")
    private Integer reminderCount;
    @TableField("last_reminder_time")
    private LocalDateTime lastReminderTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
