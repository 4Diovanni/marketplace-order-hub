package com.giovanni.orderhub.dto.order;

/**
 * Result of receiving an order: the order itself and whether it had already been received before.
 * The controller uses {@code isDuplicate} to answer {@code 200} instead of {@code 201}.
 */
public record OrderReceipt(OrderResponse order, boolean isDuplicate) {

    public static OrderReceipt created(OrderResponse order) {
        return new OrderReceipt(order, false);
    }

    public static OrderReceipt duplicate(OrderResponse order) {
        return new OrderReceipt(order, true);
    }
}
