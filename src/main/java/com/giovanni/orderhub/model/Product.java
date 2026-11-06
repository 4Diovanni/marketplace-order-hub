package com.giovanni.orderhub.model;

import com.giovanni.orderhub.exception.product.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Version
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
        // required by JPA
    }

    public Product(String sku, String name, BigDecimal price, int stockQuantity) {
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Estoque inicial não pode ser negativo.");
        }
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public void updateDetails(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    public boolean hasStockFor(int quantity) {
        return stockQuantity >= quantity;
    }

    /**
     * Adds {@code delta} to the stock (negative to remove).
     *
     * @throws InsufficientStockException if the stock would go negative; nothing changes in that case.
     */
    public void adjustStock(int delta) {
        int newQuantity = stockQuantity + delta;
        if (newQuantity < 0) {
            throw new InsufficientStockException(sku, stockQuantity, delta);
        }
        this.stockQuantity = newQuantity;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
