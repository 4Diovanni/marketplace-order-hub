package com.giovanni.orderhub.dto.order;

import com.giovanni.orderhub.enums.OrderResultCode;

/**
 * Resposta do webhook: o pedido gravado e uma mensagem pronta para o seller ver no front.
 * O {@code code} é estável para o front decidir ícone/cor; a {@code message} é o texto exibido.
 */
public record WebhookOrderResponse(String code, String message, OrderResponse order) {

    public static WebhookOrderResponse from(OrderResponse order) {
        OrderResultCode result = OrderResultCode.from(order.status());
        String message = result.formatMessage(order.externalOrderId(), order.channel());
        return new WebhookOrderResponse(result.name(), message, order);
    }
}
