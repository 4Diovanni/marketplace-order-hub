package com.giovanni.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.giovanni.orderhub.dto.order.OrderResponse;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.exception.order.UnknownSkuException;
import com.giovanni.orderhub.model.IncomingOrder;
import com.giovanni.orderhub.model.Order;
import com.giovanni.orderhub.model.Product;
import com.giovanni.orderhub.repository.OrderRepository;
import com.giovanni.orderhub.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderRecordingServiceTest {

    private static final BigDecimal PRICE = new BigDecimal("10.00");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderRecordingService orderRecordingService;

    private Product shirt;
    private Product cap;

    @BeforeEach
    void setUp() {
        shirt = new Product("CAM-001", "Camiseta", PRICE, 5);
        cap = new Product("BON-001", "Boné", PRICE, 1);
    }

    private void stubSave() {
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static IncomingOrder orderWith(IncomingOrder.Item... items) {
        return new IncomingOrder(Channel.ALFASHOP, "AS-1", "Cliente", List.of(items));
    }

    private static IncomingOrder.Item item(String sku, int quantity) {
        return new IncomingOrder.Item(sku, quantity, PRICE);
    }

    @Test
    void shouldConfirmAndDecreaseStockWhenAllItemsAreAvailable() {
        when(productRepository.findAllBySkuIn(anyCollection())).thenReturn(List.of(shirt, cap));
        stubSave();

        OrderResponse response = orderRecordingService.record(orderWith(item("CAM-001", 2), item("BON-001", 1)));

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.totalAmount()).isEqualByComparingTo("30.00");
        assertThat(shirt.getStockQuantity()).isEqualTo(3);
        assertThat(cap.getStockQuantity()).isZero();
    }

    @Test
    void shouldSetPendingStockAndKeepAllStockWhenAnyItemIsMissing() {
        when(productRepository.findAllBySkuIn(anyCollection())).thenReturn(List.of(shirt, cap));
        stubSave();

        OrderResponse response = orderRecordingService.record(orderWith(item("CAM-001", 2), item("BON-001", 2)));

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_STOCK);
        assertThat(shirt.getStockQuantity()).isEqualTo(5);
        assertThat(cap.getStockQuantity()).isEqualTo(1);
    }

    @Test
    void shouldSumRepeatedSkuLinesBeforeCheckingStock() {
        when(productRepository.findAllBySkuIn(anyCollection())).thenReturn(List.of(shirt));
        stubSave();

        OrderResponse response = orderRecordingService.record(orderWith(item("CAM-001", 3), item("CAM-001", 3)));

        assertThat(response.status()).isEqualTo(OrderStatus.PENDING_STOCK);
        assertThat(shirt.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void shouldRejectOrderWithUnknownSkuWithoutSavingAnything() {
        when(productRepository.findAllBySkuIn(anyCollection())).thenReturn(List.of(shirt));

        assertThatThrownBy(() -> orderRecordingService.record(orderWith(item("CAM-001", 1), item("XYZ-999", 1))))
                .isInstanceOf(UnknownSkuException.class)
                .hasMessageContaining("XYZ-999");
        assertThat(shirt.getStockQuantity()).isEqualTo(5);
        verify(orderRepository, never()).saveAndFlush(any());
    }
}
