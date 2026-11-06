package com.giovanni.orderhub.model;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.service.OrderService;
import java.math.BigDecimal;
import java.util.List;

/**
 * Order already converted to the internal model, regardless of the source marketplace.
 * The adapters in {@code integration} produce this record; OrderService only knows this one.
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
