package com.giovanni.orderhub.model;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        // required by JPA
    }

    private Order(IncomingOrder incoming, OrderStatus status) {
        this.channel = incoming.channel();
        this.externalOrderId = incoming.externalOrderId();
        this.customerName = incoming.customerName();
        this.status = status;
        for (IncomingOrder.Item item : incoming.items()) {
            items.add(new OrderItem(this, item.sku(), item.quantity(), item.unitPrice()));
        }
        this.totalAmount = calculateTotal();
    }

    private BigDecimal calculateTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            total = total.add(item.subtotal());
        }
        return total;
    }

    public static Order of(IncomingOrder incoming, OrderStatus status) {
        return new Order(incoming, status);
    }

    /**
     * Moves a pending order to confirmed. The caller must have reserved the stock first.
     *
     * @throws IllegalStateException if the order is not pending; confirming twice would reserve stock twice.
     */
    public void confirm() {
        if (status != OrderStatus.PENDING_STOCK) {
            throw new IllegalStateException("Pedido " + id + " não está pendente: status=" + status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    /** The same SKU can appear in more than one line; the stock check needs the total per SKU. */
    public Map<String, Integer> quantityBySku() {
        Map<String, Integer> quantityBySku = new LinkedHashMap<>();
        for (OrderItem item : items) {
            quantityBySku.merge(item.getSku(), item.getQuantity(), Integer::sum);
        }
        return quantityBySku;
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
