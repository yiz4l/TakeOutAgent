package com.yizl.healthy.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        String orderNo,
        Long merchantId,
        String merchantName,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime orderTime,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        Integer itemCount
) {
}
