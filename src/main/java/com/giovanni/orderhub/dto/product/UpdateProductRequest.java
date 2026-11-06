package com.giovanni.orderhub.dto.product;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** SKU and stock do not change here: the SKU is the product's identity and stock has its own endpoint. */
public record UpdateProductRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotNull
        @Positive
        @Digits(integer = 10, fraction = 2)
        BigDecimal price) {
}
