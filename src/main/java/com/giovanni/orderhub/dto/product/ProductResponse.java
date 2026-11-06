package com.giovanni.orderhub.dto.product;

import com.giovanni.orderhub.model.Product;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        String sku,
        String name,
        BigDecimal price,
        int stockQuantity,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getSku(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
