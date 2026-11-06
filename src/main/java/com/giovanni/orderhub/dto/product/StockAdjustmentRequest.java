package com.giovanni.orderhub.dto.product;

import jakarta.validation.constraints.NotNull;

/** A positive {@code delta} restocks; a negative one removes stock. */
public record StockAdjustmentRequest(@NotNull Integer delta) {
}
