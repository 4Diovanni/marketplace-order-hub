package com.giovanni.orderhub.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.giovanni.orderhub.support.MySqlContainerSupport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * The whole life of an order against a real MySQL, the way a seller would see it:
 * register a product, receive an order, receive the same order again, then run out of stock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderFlowIntegrationTest extends MySqlContainerSupport {

    private static final String ALFASHOP_WEBHOOK = "/api/webhooks/alfashop/orders";
    private static final int INITIAL_STOCK = 3;
    private static final int QUANTITY_PER_ORDER = 2;

    @Autowired
    private TestRestTemplate rest;

    @Test
    void shouldReserveStockOnceAndLeaveNextOrderPendingWhenStockRunsOut() {
        String sku = createProduct(INITIAL_STOCK);
        String firstOrder = alfaShopOrder(uniqueId(), sku);

        ResponseEntity<Map> received = rest.postForEntity(ALFASHOP_WEBHOOK, json(firstOrder), Map.class);
        assertThat(received.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(orderOf(received)).containsEntry("status", "CONFIRMED");
        assertThat(stockOf(sku)).isEqualTo(INITIAL_STOCK - QUANTITY_PER_ORDER);

        long alfaShopOrdersBeforeResend = countAlfaShopOrders();
        ResponseEntity<Map> resent = rest.postForEntity(ALFASHOP_WEBHOOK, json(firstOrder), Map.class);
        assertThat(resent.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orderOf(resent)).containsEntry("id", orderOf(received).get("id"));
        assertThat(countAlfaShopOrders()).isEqualTo(alfaShopOrdersBeforeResend);
        assertThat(stockOf(sku)).isEqualTo(INITIAL_STOCK - QUANTITY_PER_ORDER);

        ResponseEntity<Map> withoutStock =
                rest.postForEntity(ALFASHOP_WEBHOOK, json(alfaShopOrder(uniqueId(), sku)), Map.class);
        assertThat(withoutStock.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(withoutStock.getBody()).containsEntry("code", "ORDER_PENDING_STOCK");
        assertThat(orderOf(withoutStock)).containsEntry("status", "PENDING_STOCK");
        assertThat(stockOf(sku)).isEqualTo(INITIAL_STOCK - QUANTITY_PER_ORDER);
    }

    private String createProduct(int stock) {
        String sku = "FLX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> product = Map.of("sku", sku, "name", "Produto do fluxo", "price", 25.00,
                "stockQuantity", stock);
        ResponseEntity<Map> created = rest.postForEntity("/api/products", product, Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return sku;
    }

    private int stockOf(String sku) {
        return (Integer) rest.getForObject("/api/products/" + sku, Map.class).get("stockQuantity");
    }

    private long countAlfaShopOrders() {
        Map<String, Object> page = (Map<String, Object>) rest.getForObject("/api/orders?channel=ALFASHOP", Map.class)
                .get("page");
        return ((Number) page.get("totalElements")).longValue();
    }

    private static String uniqueId() {
        return "AS-" + UUID.randomUUID();
    }

    private static String alfaShopOrder(String externalId, String sku) {
        return """
                {"orderId":"%s","buyer":{"fullName":"Cliente"},"items":[{"sku":"%s","qty":%d,"price":25.00}]}
                """.formatted(externalId, sku, QUANTITY_PER_ORDER);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> orderOf(ResponseEntity<Map> webhookResponse) {
        return (Map<String, Object>) webhookResponse.getBody().get("order");
    }

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
