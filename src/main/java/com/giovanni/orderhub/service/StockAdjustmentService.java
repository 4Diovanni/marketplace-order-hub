package com.giovanni.orderhub.service;

import com.giovanni.orderhub.dto.product.ProductResponse;
import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.repository.OrderRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Adjusts the stock of a product and, when stock is added, confirms the pending orders that were waiting for it.
 *
 * <p>Not transactional on purpose: the adjustment commits first, then each pending order is tried in its own
 * transaction. A failure while confirming an order never undoes the adjustment the seller asked for.
 */
@Service
public class StockAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(StockAdjustmentService.class);

    /** A new order for the same product at the same time: the confirmation re-reads the stock and tries again. */
    static final int MAX_CONFIRMATION_ATTEMPTS = 3;

    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final PendingOrderConfirmationService pendingOrderConfirmationService;

    public StockAdjustmentService(ProductService productService, OrderRepository orderRepository,
            PendingOrderConfirmationService pendingOrderConfirmationService) {
        this.productService = productService;
        this.orderRepository = orderRepository;
        this.pendingOrderConfirmationService = pendingOrderConfirmationService;
    }

    /**
     * Adds {@code delta} to the stock (negative to remove). When stock is added, tries to confirm the
     * {@link OrderStatus#PENDING_STOCK} orders that contain the SKU, oldest first.
     *
     * <p>An order that still cannot be confirmed is skipped and stays pending; younger orders that fit in the
     * remaining stock are confirmed. The returned product already reflects the stock those orders reserved.
     */
    public ProductResponse adjust(String sku, int delta) {
        ProductResponse adjusted = productService.adjustStock(sku, delta);
        if (delta <= 0) {
            return adjusted;
        }

        int confirmedOrders = confirmPendingOrdersWaitingFor(sku);
        if (confirmedOrders == 0) {
            return adjusted;
        }
        return productService.findBySku(sku);
    }

    private int confirmPendingOrdersWaitingFor(String sku) {
        List<Long> pendingOrderIds =
                orderRepository.findIdsByStatusAndItemSkuInArrivalOrder(OrderStatus.PENDING_STOCK, sku);

        int confirmedOrders = 0;
        for (Long orderId : pendingOrderIds) {
            if (confirmWithRetry(orderId)) {
                confirmedOrders++;
            }
        }

        if (!pendingOrderIds.isEmpty()) {
            log.info("Pedidos pendentes reprocessados após reposição: sku={}, pendentes={}, confirmados={}",
                    sku, pendingOrderIds.size(), confirmedOrders);
        }
        return confirmedOrders;
    }

    /**
     * If every attempt loses the race, the order stays pending and is tried again on the next replenishment.
     * The error is not rethrown: the stock adjustment is already saved, and failing the request would make the
     * seller send the same adjustment twice.
     */
    private boolean confirmWithRetry(Long orderId) {
        for (int attempt = 1; attempt <= MAX_CONFIRMATION_ATTEMPTS; attempt++) {
            try {
                return pendingOrderConfirmationService.confirmIfStockAvailable(orderId);
            } catch (OptimisticLockingFailureException exception) {
                log.warn("Estoque alterado por outra operação ao confirmar pedido pendente: orderId={}, attempt={}",
                        orderId, attempt);
            }
        }
        log.warn("Pedido pendente não confirmado após {} tentativas; fica pendente até a próxima reposição: "
                + "orderId={}", MAX_CONFIRMATION_ATTEMPTS, orderId);
        return false;
    }
}
