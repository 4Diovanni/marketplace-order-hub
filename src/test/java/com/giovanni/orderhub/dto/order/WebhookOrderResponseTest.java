package com.giovanni.orderhub.dto.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class WebhookOrderResponseTest {

    private static OrderResponse orderWithStatus(OrderStatus status) {
        return new OrderResponse(1L, Channel.BETAMARKET, "BM-778", status, "Cliente",
                BigDecimal.TEN, Instant.now(), List.of());
    }

    @Test
    void shouldBuildConfirmedMessageForSeller() {
        WebhookOrderResponse response = WebhookOrderResponse.from(orderWithStatus(OrderStatus.CONFIRMED));

        assertThat(response.code()).isEqualTo("ORDER_CONFIRMED");
        assertThat(response.message()).isEqualTo("Pedido BM-778 da BetaMarket confirmado. O estoque já foi reservado.");
    }

    @Test
    void shouldBuildPendingStockMessageForSeller() {
        WebhookOrderResponse response = WebhookOrderResponse.from(orderWithStatus(OrderStatus.PENDING_STOCK));

        assertThat(response.code()).isEqualTo("ORDER_PENDING_STOCK");
        assertThat(response.message()).contains("BM-778", "BetaMarket", "aguardando estoque");
    }
}
