package com.giovanni.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giovanni.orderhub.dto.order.OrderReceipt;
import com.giovanni.orderhub.dto.order.OrderResponse;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.model.IncomingOrder;
import com.giovanni.orderhub.model.Order;
import com.giovanni.orderhub.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String EXTERNAL_ID = "AS-1";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderRecordingService orderRecordingService;

    @InjectMocks
    private OrderService orderService;

    private final IncomingOrder incoming = new IncomingOrder(Channel.ALFASHOP, EXTERNAL_ID, "Cliente",
            List.of(new IncomingOrder.Item("CAM-001", 1, new BigDecimal("10.00"))));

    private final OrderResponse savedOrder = new OrderResponse(1L, Channel.ALFASHOP, EXTERNAL_ID,
            OrderStatus.CONFIRMED, "Cliente", new BigDecimal("10.00"), null, List.of());

    private void stubExistingOrder(Optional<Order> first, Optional<Order> second) {
        when(orderRepository.findWithItemsByChannelAndExternalOrderId(Channel.ALFASHOP, EXTERNAL_ID))
                .thenReturn(first, second);
    }

    private Order storedOrder() {
        return Order.of(incoming, OrderStatus.CONFIRMED);
    }

    private static ObjectOptimisticLockingFailureException optimisticLockFailure() {
        return new ObjectOptimisticLockingFailureException("Product", 1L);
    }

    @Test
    void shouldRecordNewOrderWhenItWasNotReceivedBefore() {
        stubExistingOrder(Optional.empty(), Optional.empty());
        when(orderRecordingService.record(incoming)).thenReturn(savedOrder);

        OrderReceipt receipt = orderService.receive(incoming);

        assertThat(receipt.isDuplicate()).isFalse();
        assertThat(receipt.order()).isEqualTo(savedOrder);
    }

    @Test
    void shouldReturnOriginalOrderWithoutRecordingWhenOrderWasAlreadyReceived() {
        stubExistingOrder(Optional.of(storedOrder()), Optional.empty());

        OrderReceipt receipt = orderService.receive(incoming);

        assertThat(receipt.isDuplicate()).isTrue();
        assertThat(receipt.order().externalOrderId()).isEqualTo(EXTERNAL_ID);
        verify(orderRecordingService, never()).record(any());
    }

    @Test
    void shouldReturnOrderSavedByConcurrentRequestWhenUniqueKeyFails() {
        stubExistingOrder(Optional.empty(), Optional.of(storedOrder()));
        when(orderRecordingService.record(incoming)).thenThrow(new DataIntegrityViolationException("duplicate key"));

        OrderReceipt receipt = orderService.receive(incoming);

        assertThat(receipt.isDuplicate()).isTrue();
        assertThat(receipt.order().externalOrderId()).isEqualTo(EXTERNAL_ID);
    }

    @Test
    void shouldRethrowIntegrityErrorWhenNoOrderWithSameIdExists() {
        stubExistingOrder(Optional.empty(), Optional.empty());
        DataIntegrityViolationException failure = new DataIntegrityViolationException("other constraint");
        when(orderRecordingService.record(incoming)).thenThrow(failure);

        assertThatThrownBy(() -> orderService.receive(incoming)).isSameAs(failure);
    }

    @Test
    void shouldRetryWhenStockWasChangedByAnotherOrderAtTheSameTime() {
        stubExistingOrder(Optional.empty(), Optional.empty());
        when(orderRecordingService.record(incoming)).thenThrow(optimisticLockFailure()).thenReturn(savedOrder);

        OrderReceipt receipt = orderService.receive(incoming);

        assertThat(receipt.order()).isEqualTo(savedOrder);
        verify(orderRecordingService, times(2)).record(incoming);
    }

    @Test
    void shouldGiveUpAfterMaxAttemptsWhenStockKeepsChanging() {
        stubExistingOrder(Optional.empty(), Optional.empty());
        when(orderRecordingService.record(incoming)).thenThrow(optimisticLockFailure());

        assertThatThrownBy(() -> orderService.receive(incoming))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        verify(orderRecordingService, times(OrderService.MAX_STOCK_UPDATE_ATTEMPTS)).record(incoming);
    }
}
