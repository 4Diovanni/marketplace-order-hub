package com.giovanni.orderhub.dto.product;

import jakarta.validation.constraints.NotNull;

/** {@code delta} positivo repõe estoque, negativo retira. */
public record StockAdjustmentRequest(@NotNull Integer delta) {
}
