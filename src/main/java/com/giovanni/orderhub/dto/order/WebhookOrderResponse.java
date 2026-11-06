package com.giovanni.orderhub.dto.order;

import com.giovanni.orderhub.enums.OrderResultCode;

/**
 * Webhook response: the saved order and a message ready for the seller to see in the front end.
 * The {@code code} is stable so the front end can pick icon/color; the {@code message} is the displayed text.
 */
public record WebhookOrderResponse(String code, String message, OrderResponse order) {

    public static WebhookOrderResponse from(OrderResponse order) {
        return withResult(OrderResultCode.from(order.status()), order);
    }

    /** The marketplace sent an order that was already saved: nothing changed, the original order goes back. */
    public static WebhookOrderResponse alreadyReceived(OrderResponse order) {
        return withResult(OrderResultCode.ORDER_ALREADY_RECEIVED, order);
    }

    private static WebhookOrderResponse withResult(OrderResultCode result, OrderResponse order) {
        String message = result.formatMessage(order.externalOrderId(), order.channel());
        return new WebhookOrderResponse(result.name(), message, order);
    }
}
