package com.giovanni.orderhub.dto.order;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.model.Order;
import java.math.BigDecimal;
import java.time.Instant;

/** Short version for listings, without items (avoids loading the items of every order on the page). */
public record OrderSummaryResponse(
        Long id,
        Channel channel,
        String externalOrderId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant receivedAt) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getChannel(),
                order.getExternalOrderId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getReceivedAt());
    }
}
