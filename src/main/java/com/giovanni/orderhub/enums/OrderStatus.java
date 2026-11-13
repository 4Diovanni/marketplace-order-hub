package com.giovanni.orderhub.enums;

public enum OrderStatus {
    /** Every item had stock and the stock was already reserved. */
    CONFIRMED,
    /**
     * Some item did not have enough stock; nothing was reserved.
     * Confirmed automatically, in order of arrival, when the missing stock is replenished.
     */
    PENDING_STOCK
}
