package com.giovanni.orderhub.enums;

/** Success messages of the order webhook, so the seller can see the result of each imported order. */
public enum OrderResultCode {

    ORDER_CONFIRMED("Pedido %s da %s confirmado. O estoque já foi reservado."),
    ORDER_PENDING_STOCK("Pedido %s da %s recebido, mas aguardando estoque. Nenhum item foi reservado ainda.");

    private final String messageTemplate;

    OrderResultCode(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public static OrderResultCode from(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> ORDER_CONFIRMED;
            case PENDING_STOCK -> ORDER_PENDING_STOCK;
        };
    }

    public String formatMessage(String externalOrderId, Channel channel) {
        return messageTemplate.formatted(externalOrderId, channel.getDisplayName());
    }
}
