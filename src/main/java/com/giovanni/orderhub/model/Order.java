package com.giovanni.orderhub.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Channel channel;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
        // exigido pelo JPA
    }

    private Order(IncomingOrder incoming, OrderStatus status) {
        this.channel = incoming.channel();
        this.externalOrderId = incoming.externalOrderId();
        this.customerName = incoming.customerName();
        this.status = status;
        incoming.items().forEach(item ->
                items.add(new OrderItem(this, item.sku(), item.quantity(), item.unitPrice())));
        this.totalAmount = items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static Order of(IncomingOrder incoming, OrderStatus status) {
        return new Order(incoming, status);
    }

    public Long getId() {
        return id;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
