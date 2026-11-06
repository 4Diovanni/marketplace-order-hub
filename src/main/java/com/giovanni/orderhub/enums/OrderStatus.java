package com.giovanni.orderhub.enums;

public enum OrderStatus {
    /** Every item had stock and the stock was already reserved. */
    CONFIRMED,
    /** Some item did not have enough stock; nothing was reserved. */
    PENDING_STOCK
}
