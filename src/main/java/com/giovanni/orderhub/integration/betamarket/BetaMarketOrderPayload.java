package com.giovanni.orderhub.integration.betamarket;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** Formato fictício da BetaMarket: aninhado, snake_case, valores em centavos e nome do cliente em dois campos. */
public record BetaMarketOrderPayload(@NotNull @Valid OrderData order) {

    public record OrderData(
            @JsonProperty("external_id") @NotBlank String externalId,
            @NotNull @Valid Customer customer,
            @NotEmpty List<@NotNull @Valid Line> lines) {
    }

    public record Customer(
            @JsonProperty("first_name") @NotBlank String firstName,
            @JsonProperty("last_name") String lastName) {
    }

    public record Line(
            @JsonProperty("product_code") @NotBlank String productCode,
            @NotNull @Positive Integer amount,
            @JsonProperty("unit_price_cents") @NotNull @Positive Long unitPriceCents) {
    }
}
