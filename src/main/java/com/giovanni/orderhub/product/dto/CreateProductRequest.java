package com.giovanni.orderhub.product.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z0-9-]+$", message = "deve conter só letras maiúsculas, números e hífen")
        String sku,

        @NotBlank
        @Size(max = 150)
        String name,

        @NotNull
        @Positive
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull
        @PositiveOrZero
        Integer stockQuantity) {
}
