package com.giovanni.orderhub.service;

import com.giovanni.orderhub.dto.order.OrderReceipt;
import com.giovanni.orderhub.dto.order.OrderResponse;
import com.giovanni.orderhub.dto.order.OrderSummaryResponse;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.exception.order.OrderNotFoundException;
import com.giovanni.orderhub.model.IncomingOrder;
import com.giovanni.orderhub.repository.OrderRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /** Two orders for the same product at the same time: the loser re-reads the stock and tries again. */
    static final int MAX_STOCK_UPDATE_ATTEMPTS = 3;

    private final OrderRepository orderRepository;
    private final OrderRecordingService orderRecordingService;

    public OrderService(OrderRepository orderRepository, OrderRecordingService orderRecordingService) {
        this.orderRepository = orderRepository;
        this.orderRecordingService = orderRecordingService;
    }

    /**
     * Receives an order from a marketplace exactly once.
     *
     * <p>An order already received (same channel and external id) is returned as it is, without touching stock.
     * Two copies arriving at the same time are caught by the unique key in the database: the second one also
     * gets the original order back.
     */
    public OrderReceipt receive(IncomingOrder incoming) {
        Optional<OrderResponse> existingOrder = findExisting(incoming);
        if (existingOrder.isPresent()) {
            return duplicateOf(existingOrder.get());
        }

        try {
            return OrderReceipt.created(recordWithRetry(incoming));
        } catch (DataIntegrityViolationException exception) {
            OrderResponse orderSavedByOtherRequest = findExisting(incoming).orElseThrow(() -> exception);
            return duplicateOf(orderSavedByOtherRequest);
        }
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> search(OrderStatus status, Channel channel, Pageable pageable) {
        return orderRepository.search(status, channel, pageable).map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return orderRepository.findWithItemsById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private Optional<OrderResponse> findExisting(IncomingOrder incoming) {
        return orderRepository.findWithItemsByChannelAndExternalOrderId(incoming.channel(), incoming.externalOrderId())
                .map(OrderResponse::from);
    }

    /** The last attempt is outside the loop, so its failure reaches the caller. */
    private OrderResponse recordWithRetry(IncomingOrder incoming) {
        for (int attempt = 1; attempt < MAX_STOCK_UPDATE_ATTEMPTS; attempt++) {
            try {
                return orderRecordingService.record(incoming);
            } catch (OptimisticLockingFailureException exception) {
                log.warn("Estoque alterado por outro pedido ao mesmo tempo, tentando de novo: channel={}, "
                        + "externalOrderId={}, attempt={}", incoming.channel(), incoming.externalOrderId(), attempt);
            }
        }
        return orderRecordingService.record(incoming);
    }

    private OrderReceipt duplicateOf(OrderResponse existingOrder) {
        log.info("Pedido duplicado ignorado: channel={}, externalOrderId={}, orderId={}",
                existingOrder.channel(), existingOrder.externalOrderId(), existingOrder.id());
        return OrderReceipt.duplicate(existingOrder);
    }
}
