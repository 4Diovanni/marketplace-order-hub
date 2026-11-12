package com.giovanni.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.giovanni.orderhub.model.Product;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockReservationServiceTest {

    private static final BigDecimal PRICE = new BigDecimal("10.00");

    private final StockReservationService stockReservationService = new StockReservationService();

    private Product shirt;
    private Product cap;

    @BeforeEach
    void setUp() {
        shirt = new Product("CAM-001", "Camiseta", PRICE, 5);
        cap = new Product("BON-001", "Boné", PRICE, 1);
    }

    @Test
    void shouldReserveEverySkuWhenAllHaveStock() {
        boolean reserved = stockReservationService.reserveAllOrNothing(
                Map.of("CAM-001", 2, "BON-001", 1), Map.of("CAM-001", shirt, "BON-001", cap));

        assertThat(reserved).isTrue();
        assertThat(shirt.getStockQuantity()).isEqualTo(3);
        assertThat(cap.getStockQuantity()).isZero();
    }

    @Test
    void shouldReserveNothingWhenAnySkuIsMissingStock() {
        boolean reserved = stockReservationService.reserveAllOrNothing(
                Map.of("CAM-001", 2, "BON-001", 2), Map.of("CAM-001", shirt, "BON-001", cap));

        assertThat(reserved).isFalse();
        assertThat(shirt.getStockQuantity()).isEqualTo(5);
        assertThat(cap.getStockQuantity()).isEqualTo(1);
    }

    @Test
    void shouldReserveNothingWhenProductIsNotLoaded() {
        boolean reserved = stockReservationService.reserveAllOrNothing(
                Map.of("CAM-001", 1, "XYZ-999", 1), Map.of("CAM-001", shirt));

        assertThat(reserved).isFalse();
        assertThat(shirt.getStockQuantity()).isEqualTo(5);
    }
}
