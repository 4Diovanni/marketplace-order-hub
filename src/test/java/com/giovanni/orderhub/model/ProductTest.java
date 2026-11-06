package com.giovanni.orderhub.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giovanni.orderhub.exception.InsufficientStockException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {

    private Product productWithStock(int quantity) {
        return new Product("CAM-001", "Camiseta", new BigDecimal("59.90"), quantity);
    }

    @Test
    void shouldIncreaseStockWhenDeltaIsPositive() {
        Product product = productWithStock(10);

        product.adjustStock(5);

        assertThat(product.getStockQuantity()).isEqualTo(15);
    }

    @Test
    void shouldDecreaseStockDownToZero() {
        Product product = productWithStock(10);

        product.adjustStock(-10);

        assertThat(product.getStockQuantity()).isZero();
    }

    @Test
    void shouldRejectAdjustmentThatMakesStockNegativeAndKeepCurrentStock() {
        Product product = productWithStock(3);

        assertThatThrownBy(() -> product.adjustStock(-4))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("CAM-001");
        assertThat(product.getStockQuantity()).isEqualTo(3);
    }

    @Test
    void shouldRejectNegativeInitialStock() {
        assertThatThrownBy(() -> productWithStock(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
