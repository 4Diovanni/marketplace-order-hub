package com.giovanni.orderhub.enums;

public enum OrderStatus {
    /** Todos os itens tinham estoque e o estoque já foi baixado. */
    CONFIRMED,
    /** Algum item não tinha estoque suficiente; nada foi baixado. */
    PENDING_STOCK
}
