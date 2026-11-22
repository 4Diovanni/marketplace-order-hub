package com.giovanni.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.giovanni.orderhub.dto.product.ProductResponse;
import com.giovanni.orderhub.enums.OrderStatus;
import com.giovanni.orderhub.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class StockAdjustmentServiceTest {

    private static final String SKU = "CAM-001";
    private static final long OLDEST_ORDER_ID = 1L;
    private static final long MIDDLE_ORDER_ID = 2L;
    private static final long NEWEST_ORDER_ID = 3L;

    @Mock
    private ProductService productService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PendingOrderConfirmationService pendingOrderConfirmationService;

    @InjectMocks
    private StockAdjustmentService stockAdjustmentService;

    private static ProductResponse productWithStock(int stock) {
        return new ProductResponse(SKU, "Camiseta", new BigDecimal("59.90"), stock, null, null);
    }

    private void stubPendingOrders(Long... orderIds) {
        when(orderRepository.findIdsByStatusAndItemSkuInArrivalOrder(OrderStatus.PENDING_STOCK, SKU))
                .thenReturn(List.of(orderIds));
    }

    @Test
    void shouldNotReprocessPendingOrdersWhenStockIsRemoved() {
        when(productService.adjustStock(SKU, -2)).thenReturn(productWithStock(3));

        ProductResponse response = stockAdjustmentService.adjust(SKU, -2);

        assertThat(response.stockQuantity()).isEqualTo(3);
        verifyNoInteractions(orderRepository, pendingOrderConfirmationService);
    }

    @Test
    void shouldTryPendingOrdersOldestFirstAndKeepGoingWhenOneDoesNotFit() {
        when(productService.adjustStock(SKU, 3)).thenReturn(productWithStock(3));
        stubPendingOrders(OLDEST_ORDER_ID, MIDDLE_ORDER_ID, NEWEST_ORDER_ID);
        when(pendingOrderConfirmationService.confirmIfStockAvailable(OLDEST_ORDER_ID)).thenReturn(true);
        when(pendingOrderConfirmationService.confirmIfStockAvailable(MIDDLE_ORDER_ID)).thenReturn(false);
        when(pendingOrderConfirmationService.confirmIfStockAvailable(NEWEST_ORDER_ID)).thenReturn(true);
        when(productService.findBySku(SKU)).thenReturn(productWithStock(0));

        ProductResponse response = stockAdjustmentService.adjust(SKU, 3);

        InOrder arrivalOrder = inOrder(pendingOrderConfirmationService);
        arrivalOrder.verify(pendingOrderConfirmationService).confirmIfStockAvailable(OLDEST_ORDER_ID);
        arrivalOrder.verify(pendingOrderConfirmationService).confirmIfStockAvailable(MIDDLE_ORDER_ID);
        arrivalOrder.verify(pendingOrderConfirmationService).confirmIfStockAvailable(NEWEST_ORDER_ID);
        assertThat(response.stockQuantity()).isZero();
    }

    @Test
    void shouldReturnAdjustedProductWithoutReloadingWhenNoOrderIsConfirmed() {
        when(productService.adjustStock(SKU, 1)).thenReturn(productWithStock(1));
        stubPendingOrders(OLDEST_ORDER_ID);
        when(pendingOrderConfirmationService.confirmIfStockAvailable(OLDEST_ORDER_ID)).thenReturn(false);

        ProductResponse response = stockAdjustmentService.adjust(SKU, 1);

        assertThat(response.stockQuantity()).isEqualTo(1);
        verify(productService, never()).findBySku(any());
    }

    @Test
    void shouldRetryConfirmationWhenStockChangedAtTheSameTime() {
        when(productService.adjustStock(SKU, 2)).thenReturn(productWithStock(2));
        stubPendingOrders(OLDEST_ORDER_ID);
        when(pendingOrderConfirmationService.confirmIfStockAvailable(OLDEST_ORDER_ID))
                .thenThrow(new OptimisticLockingFailureException("concurrent update"))
                .thenReturn(true);
        when(productService.findBySku(SKU)).thenReturn(productWithStock(0));

        ProductResponse response = stockAdjustmentService.adjust(SKU, 2);

        verify(pendingOrderConfirmationService, times(2)).confirmIfStockAvailable(OLDEST_ORDER_ID);
        assertThat(response.stockQuantity()).isZero();
    }

    @Test
    void shouldKeepAdjustmentAndLeaveOrderPendingWhenEveryRetryLosesTheRace() {
        when(productService.adjustStock(SKU, 2)).thenReturn(productWithStock(2));
        stubPendingOrders(OLDEST_ORDER_ID);
        when(pendingOrderConfirmationService.confirmIfStockAvailable(anyLong()))
                .thenThrow(new OptimisticLockingFailureException("concurrent update"));

        ProductResponse response = stockAdjustmentService.adjust(SKU, 2);

        verify(pendingOrderConfirmationService, times(StockAdjustmentService.MAX_CONFIRMATION_ATTEMPTS))
                .confirmIfStockAvailable(OLDEST_ORDER_ID);
        assertThat(response.stockQuantity()).isEqualTo(2);
    }
}
