package com.giovanni.orderhub.dto.product;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** SKU e estoque não mudam por aqui: o SKU é a identidade do produto e o estoque tem endpoint próprio. */
public record UpdateProductRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotNull
        @Positive
        @Digits(integer = 10, fraction = 2)
        BigDecimal price) {
}
