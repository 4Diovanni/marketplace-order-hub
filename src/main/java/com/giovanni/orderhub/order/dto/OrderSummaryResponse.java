package com.giovanni.orderhub.order.dto;

import com.giovanni.orderhub.order.Channel;
import com.giovanni.orderhub.order.Order;
import com.giovanni.orderhub.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Versão resumida para listagem, sem itens (evita carregar os itens de cada pedido da página). */
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
