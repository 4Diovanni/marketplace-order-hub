package com.giovanni.orderhub.service;

import com.giovanni.orderhub.dto.order.OrderResponse;
import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.exception.order.UnknownSkuException;
import com.giovanni.orderhub.model.IncomingOrder;
import com.giovanni.orderhub.model.Order;
import com.giovanni.orderhub.model.Product;
import com.giovanni.orderhub.repository.OrderRepository;
import com.giovanni.orderhub.repository.ProductRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves a new order and applies the all-or-nothing stock rule, in a single transaction.
 *
 * <p>Kept apart from {@link OrderService} so that each retry runs in a fresh transaction:
 * a transaction that hit an optimistic lock or a unique key failure cannot be reused.
 */
@Service
public class OrderRecordingService {

    private static final Logger log = LoggerFactory.getLogger(OrderRecordingService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderRecordingService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Records an order using the all-or-nothing rule:
     * if every item has stock, reserves it and confirms; if any is missing, reserves nothing and leaves it pending.
     *
     * @throws UnknownSkuException if any SKU in the order is not in the catalog (nothing is saved).
     */
    @Transactional
    public OrderResponse record(IncomingOrder incoming) {
        Map<String, Integer> quantityBySku = totalQuantityPerSku(incoming.items());
        Map<String, Product> productBySku = findProducts(quantityBySku.keySet());

        OrderStatus status = reserveStockIfAvailable(quantityBySku, productBySku);

        Order order = orderRepository.saveAndFlush(Order.of(incoming, status));
        log.info("Pedido recebido: channel={}, externalOrderId={}, status={}, orderId={}",
                order.getChannel(), order.getExternalOrderId(), order.getStatus(), order.getId());
        return OrderResponse.from(order);
    }

    /** The same SKU can appear in more than one order line; the stock check needs the total per SKU. */
    private Map<String, Integer> totalQuantityPerSku(List<IncomingOrder.Item> items) {
        Map<String, Integer> quantityBySku = new LinkedHashMap<>();
        for (IncomingOrder.Item item : items) {
            quantityBySku.merge(item.sku(), item.quantity(), Integer::sum);
        }
        return quantityBySku;
    }

    private Map<String, Product> findProducts(Set<String> skus) {
        Map<String, Product> productBySku = new HashMap<>();
        for (Product product : productRepository.findAllBySkuIn(skus)) {
            productBySku.put(product.getSku(), product);
        }

        List<String> unknownSkus = new ArrayList<>(skus);
        unknownSkus.removeAll(productBySku.keySet());
        if (!unknownSkus.isEmpty()) {
            throw new UnknownSkuException(unknownSkus);
        }
        return productBySku;
    }

    /** Reserves stock for every item, or for none. */
    private OrderStatus reserveStockIfAvailable(Map<String, Integer> quantityBySku, Map<String, Product> productBySku) {
        if (!hasStockForAllItems(quantityBySku, productBySku)) {
            return OrderStatus.PENDING_STOCK;
        }

        for (Map.Entry<String, Integer> entry : quantityBySku.entrySet()) {
            Product product = productBySku.get(entry.getKey());
            product.adjustStock(-entry.getValue());
        }
        return OrderStatus.CONFIRMED;
    }

    private boolean hasStockForAllItems(Map<String, Integer> quantityBySku, Map<String, Product> productBySku) {
        for (Map.Entry<String, Integer> entry : quantityBySku.entrySet()) {
            Product product = productBySku.get(entry.getKey());
            if (!product.hasStockFor(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
