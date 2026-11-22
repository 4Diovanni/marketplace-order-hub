package com.giovanni.orderhub.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final BigDecimal PRICE = new BigDecimal("10.00");

    private static Order orderWith(OrderStatus status, IncomingOrder.Item... items) {
        return Order.of(new IncomingOrder(Channel.ALFASHOP, "AS-1", "Cliente", List.of(items)), status);
    }

    private static IncomingOrder.Item item(String sku, int quantity) {
        return new IncomingOrder.Item(sku, quantity, PRICE);
    }

    @Test
    void shouldConfirmPendingOrder() {
        Order order = orderWith(OrderStatus.PENDING_STOCK, item("CAM-001", 1));

        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldRefuseToConfirmOrderThatIsAlreadyConfirmed() {
        Order order = orderWith(OrderStatus.CONFIRMED, item("CAM-001", 1));

        assertThatThrownBy(order::confirm).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldSumQuantitiesOfRepeatedSkuLines() {
        Order order = orderWith(OrderStatus.PENDING_STOCK, item("CAM-001", 2), item("BON-001", 1), item("CAM-001", 3));

        assertThat(order.quantityBySku()).containsExactly(
                Map.entry("CAM-001", 5),
                Map.entry("BON-001", 1));
    }
}
