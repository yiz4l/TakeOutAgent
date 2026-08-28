package com.yizl.healthy.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ReminderResponse(
        Long orderId,
        Integer reminderCount,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lastReminderTime
) {
}
