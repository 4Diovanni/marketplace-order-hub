package com.giovanni.orderhub.model;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.service.OrderService;
import java.math.BigDecimal;
import java.util.List;

/**
 * Pedido já convertido para o modelo interno, independente do marketplace de origem.
 * Os adapters em {@code integration} produzem este record; o OrderService só conhece ele.
 */
public record IncomingOrder(
        Channel channel,
        String externalOrderId,
        String customerName,
        List<Item> items) {

    public IncomingOrder {
        items = List.copyOf(items);
    }

    public record Item(String sku, int quantity, BigDecimal unitPrice) {
    }
}
