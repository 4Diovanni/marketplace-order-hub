package com.giovanni.orderhub.order.dto;

import com.giovanni.orderhub.order.Channel;
import com.giovanni.orderhub.order.Order;
import com.giovanni.orderhub.order.OrderItem;
import com.giovanni.orderhub.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Channel channel,
        String externalOrderId,
        OrderStatus status,
        String customerName,
        BigDecimal totalAmount,
        Instant receivedAt,
        List<Item> items) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getChannel(),
                order.getExternalOrderId(),
                order.getStatus(),
                order.getCustomerName(),
                order.getTotalAmount(),
                order.getReceivedAt(),
                order.getItems().stream().map(Item::from).toList());
    }

    public record Item(String sku, int quantity, BigDecimal unitPrice) {

        static Item from(OrderItem item) {
            return new Item(item.getSku(), item.getQuantity(), item.getUnitPrice());
        }
    }
}
