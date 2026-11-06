package com.giovanni.orderhub.integration.alfashop;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/** Formato fictício da AlfaShop: plano, camelCase, valores em reais e nome do cliente em um campo. */
public record AlfaShopOrderPayload(
        @NotBlank String orderId,
        @NotNull @Valid Buyer buyer,
        @NotEmpty List<@NotNull @Valid Item> items) {

    public record Buyer(@NotBlank String fullName) {
    }

    public record Item(
            @NotBlank String sku,
            @NotNull @Positive Integer qty,
            @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal price) {
    }
}
