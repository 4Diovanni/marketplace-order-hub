package com.giovanni.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.model.IncomingOrder;
import com.giovanni.orderhub.model.Order;
import com.giovanni.orderhub.model.Product;
import com.giovanni.orderhub.repository.OrderRepository;
import com.giovanni.orderhub.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingOrderConfirmationServiceTest {

    private static final long ORDER_ID = 7L;
    private static final BigDecimal PRICE = new BigDecimal("10.00");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Spy
    private StockReservationService stockReservationService = new StockReservationService();

    @InjectMocks
    private PendingOrderConfirmationService pendingOrderConfirmationService;

    private Product shirt;
    private Product cap;

    @BeforeEach
    void setUp() {
        shirt = new Product("CAM-001", "Camiseta", PRICE, 5);
        cap = new Product("BON-001", "Boné", PRICE, 1);
    }

    private static Order orderWith(OrderStatus status, IncomingOrder.Item... items) {
        return Order.of(new IncomingOrder(Channel.BETAMARKET, "BM-1", "Cliente", List.of(items)), status);
    }

    private static IncomingOrder.Item item(String sku, int quantity) {
        return new IncomingOrder.Item(sku, quantity, PRICE);
    }

    private void stubLockedOrder(Order order) {
        when(orderRepository.findLockedWithItemsById(ORDER_ID)).thenReturn(Optional.of(order));
    }

    @Test
    void shouldConfirmPendingOrderAndReserveStockWhenEveryItemHasStock() {
        Order order = orderWith(OrderStatus.PENDING_STOCK, item("CAM-001", 2), item("BON-001", 1));
        stubLockedOrder(order);
        when(productRepository.findAllBySkuIn(anyCollection())).thenReturn(List.of(shirt, cap));

        boolean confirmed = pendingOrderConfirmationService.confirmIfStockAvailable(ORDER_ID);

        assertThat(confirmed).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(shirt.getStockQuantity()).isEqualTo(3);
        assertThat(cap.getStockQuantity()).isZero();
        verify(orderRepository).saveAndFlush(order);
    }

    @Test
    void shouldKeepOrderPendingAndStockUntouchedWhenAnyItemIsStillMissing() {
        Order order = orderWith(OrderStatus.PENDING_STOCK, item("CAM-001", 2), item("BON-001", 2));
        stubLockedOrder(order);
        when(productRepository.findAllBySkuIn(anyCollection())).thenReturn(List.of(shirt, cap));

        boolean confirmed = pendingOrderConfirmationService.confirmIfStockAvailable(ORDER_ID);

        assertThat(confirmed).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_STOCK);
        assertThat(shirt.getStockQuantity()).isEqualTo(5);
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldKeepOrderPendingWhenOneOfItsProductsWasDeleted() {
        Order order = orderWith(OrderStatus.PENDING_STOCK, item("CAM-001", 1), item("APAGADO-1", 1));
        stubLockedOrder(order);
        when(productRepository.findAllBySkuIn(anyCollection())).thenReturn(List.of(shirt));

        boolean confirmed = pendingOrderConfirmationService.confirmIfStockAvailable(ORDER_ID);

        assertThat(confirmed).isFalse();
        assertThat(shirt.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldIgnoreOrderThatIsNoLongerPending() {
        Order order = orderWith(OrderStatus.CONFIRMED, item("CAM-001", 1));
        stubLockedOrder(order);

        boolean confirmed = pendingOrderConfirmationService.confirmIfStockAvailable(ORDER_ID);

        assertThat(confirmed).isFalse();
        verify(productRepository, never()).findAllBySkuIn(anyCollection());
    }

    @Test
    void shouldReturnFalseWhenOrderDoesNotExist() {
        when(orderRepository.findLockedWithItemsById(ORDER_ID)).thenReturn(Optional.empty());

        assertThat(pendingOrderConfirmationService.confirmIfStockAvailable(ORDER_ID)).isFalse();
    }
}
