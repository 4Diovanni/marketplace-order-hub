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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Pending orders confirmed by a stock replenishment, against a real MySQL (issue #14). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StockReplenishmentIntegrationTest extends MySqlContainerSupport {

    private static final String ALFASHOP_WEBHOOK = "/api/webhooks/alfashop/orders";

    @Autowired
    private TestRestTemplate rest;

    @Test
    void shouldConfirmPendingOrdersOldestFirstAndSkipTheOneThatDoesNotFit() {
        String sku = createProductWithStock(0);
        long oldestNeedsTwo = receivePendingOrder(item(sku, 2));
        long middleNeedsTwo = receivePendingOrder(item(sku, 2));
        long newestNeedsOne = receivePendingOrder(item(sku, 1));

        ResponseEntity<Map> replenished = adjustStock(sku, 3);

        assertThat(replenished.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replenished.getBody()).containsEntry("stockQuantity", 0);
        assertThat(statusOf(oldestNeedsTwo)).isEqualTo("CONFIRMED");
        assertThat(statusOf(middleNeedsTwo)).isEqualTo("PENDING_STOCK");
        assertThat(statusOf(newestNeedsOne)).isEqualTo("CONFIRMED");
    }

    @Test
    void shouldConfirmMultiItemOrderOnlyWhenEveryItemHasStock() {
        String shirt = createProductWithStock(0);
        String cap = createProductWithStock(0);
        long orderId = receivePendingOrder(item(shirt, 1) + "," + item(cap, 1));

        adjustStock(shirt, 1);

        assertThat(statusOf(orderId)).isEqualTo("PENDING_STOCK");
        assertThat(stockOf(shirt)).isEqualTo(1);

        adjustStock(cap, 1);

        assertThat(statusOf(orderId)).isEqualTo("CONFIRMED");
        assertThat(stockOf(shirt)).isZero();
        assertThat(stockOf(cap)).isZero();
    }

    @Test
    void shouldNotTouchPendingOrdersWhenStockIsRemoved() {
        String sku = createProductWithStock(1);
        long orderId = receivePendingOrder(item(sku, 2));

        ResponseEntity<Map> removed = adjustStock(sku, -1);

        assertThat(removed.getBody()).containsEntry("stockQuantity", 0);
        assertThat(statusOf(orderId)).isEqualTo("PENDING_STOCK");
    }

    private String createProductWithStock(int stock) {
        String sku = "REP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> product = Map.of("sku", sku, "name", "Produto de reposição", "price", 10.00,
                "stockQuantity", stock);
        ResponseEntity<Map> created = rest.postForEntity("/api/products", product, Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return sku;
    }

    private long receivePendingOrder(String itemsJson) {
        String body = """
                {"orderId":"AS-%s","buyer":{"fullName":"Cliente"},"items":[%s]}
                """.formatted(UUID.randomUUID(), itemsJson);
        ResponseEntity<Map> received = rest.postForEntity(ALFASHOP_WEBHOOK, json(body), Map.class);
        assertThat(received.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?, ?> order = (Map<?, ?>) received.getBody().get("order");
        assertThat(order.get("status")).isEqualTo("PENDING_STOCK");
        return ((Number) order.get("id")).longValue();
    }

    private static String item(String sku, int quantity) {
        return """
                {"sku":"%s","qty":%d,"price":10.00}""".formatted(sku, quantity);
    }

    private ResponseEntity<Map> adjustStock(String sku, int delta) {
        return rest.exchange("/api/products/" + sku + "/stock", HttpMethod.PATCH, json("{\"delta\":" + delta + "}"),
                Map.class);
    }

    private String statusOf(long orderId) {
        return (String) rest.getForObject("/api/orders/" + orderId, Map.class).get("status");
    }

    private int stockOf(String sku) {
        return (Integer) rest.getForObject("/api/products/" + sku, Map.class).get("stockQuantity");
    }

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
