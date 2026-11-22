package com.giovanni.orderhub.service;

import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.model.Order;
import com.giovanni.orderhub.model.Product;
import com.giovanni.orderhub.repository.OrderRepository;
import com.giovanni.orderhub.repository.ProductRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tries to confirm one pending order, in its own transaction.
 *
 * <p>Kept apart from {@link StockAdjustmentService} for the same reason as {@link OrderRecordingService}:
 * each order (and each retry) needs a fresh transaction, so one failure does not undo the others.
 */
@Service
public class PendingOrderConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(PendingOrderConfirmationService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockReservationService stockReservationService;

    public PendingOrderConfirmationService(OrderRepository orderRepository, ProductRepository productRepository,
            StockReservationService stockReservationService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockReservationService = stockReservationService;
    }

    /**
     * Confirms the order if it is still pending and every item now has stock (all-or-nothing rule).
     *
     * @return {@code true} if the order was confirmed and its stock reserved; {@code false} if nothing changed
     */
    @Transactional
    public boolean confirmIfStockAvailable(Long orderId) {
        Optional<Order> lockedOrder = orderRepository.findLockedWithItemsById(orderId);
        if (lockedOrder.isEmpty()) {
            return false;
        }
        Order order = lockedOrder.get();
        if (order.getStatus() != OrderStatus.PENDING_STOCK) {
            return false;
        }

        Map<String, Integer> quantityBySku = order.quantityBySku();
        Map<String, Product> productBySku = findProducts(quantityBySku.keySet());
        if (!stockReservationService.reserveAllOrNothing(quantityBySku, productBySku)) {
            return false;
        }

        order.confirm();
        orderRepository.saveAndFlush(order);
        log.info("Pedido pendente confirmado após reposição de estoque: channel={}, externalOrderId={}, orderId={}",
                order.getChannel(), order.getExternalOrderId(), order.getId());
        return true;
    }

    /** A product deleted after the order arrived is simply missing here; the order then stays pending. */
    private Map<String, Product> findProducts(Set<String> skus) {
        Map<String, Product> productBySku = new HashMap<>();
        for (Product product : productRepository.findAllBySkuIn(skus)) {
            productBySku.put(product.getSku(), product);
        }
        return productBySku;
    }
}
