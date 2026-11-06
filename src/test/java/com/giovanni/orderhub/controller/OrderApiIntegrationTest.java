package com.giovanni.orderhub.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.giovanni.orderhub.support.MySqlContainerSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** End-to-end order flow: webhook → mapper → stock rule → MySQL → query. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiIntegrationTest extends MySqlContainerSupport {

    private static final int CONCURRENT_REQUESTS = 5;
    private static final int TIMEOUT_SECONDS = 30;

    @Autowired
    private TestRestTemplate rest;

    private int stockOf(String sku) {
        return (Integer) rest.getForObject("/api/products/" + sku, Map.class).get("stockQuantity");
    }

    private static String uniqueId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String createProductWithStock(int stock) {
        String sku = "TST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> product = Map.of("sku", sku, "name", "Produto de teste", "price", 10.00,
                "stockQuantity", stock);
        ResponseEntity<Map> created = rest.postForEntity("/api/products", product, Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return sku;
    }

    private static String alfaShopOrder(String externalId, String sku) {
        return """
                {"orderId":"%s","buyer":{"fullName":"Cliente"},"items":[{"sku":"%s","qty":1,"price":10.00}]}
                """.formatted(externalId, sku);
    }

    /** Fires every request at the same moment, so they really compete for the same rows. */
    private List<ResponseEntity<Map>> postAllAtOnce(List<String> bodies) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(bodies.size());
        CountDownLatch startSignal = new CountDownLatch(1);
        try {
            List<Future<ResponseEntity<Map>>> futures = new ArrayList<>();
            for (String body : bodies) {
                futures.add(executor.submit(() -> {
                    startSignal.await();
                    return rest.postForEntity("/api/webhooks/alfashop/orders", json(body), Map.class);
                }));
            }
            startSignal.countDown();

            List<ResponseEntity<Map>> responses = new ArrayList<>();
            for (Future<ResponseEntity<Map>> future : futures) {
                responses.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return responses;
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldConfirmAlfaShopOrderAndDecreaseStock() {
        int stockBefore = stockOf("TEN-001");
        String body = """
                {"orderId":"%s","buyer":{"fullName":"Cliente Teste"},
                 "items":[{"sku":"TEN-001","qty":2,"price":299.90}]}
                """.formatted(uniqueId("AS"));

        ResponseEntity<Map> response = rest.postForEntity("/api/webhooks/alfashop/orders", json(body), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .containsEntry("code", "ORDER_CONFIRMED")
                .extractingByKey("message").asString().contains("confirmado", "AlfaShop");
        assertThat(orderOf(response)).containsEntry("status", "CONFIRMED").containsEntry("channel", "ALFASHOP");
        assertThat(response.getHeaders().getLocation().getPath()).startsWith("/api/orders/");
        assertThat(stockOf("TEN-001")).isEqualTo(stockBefore - 2);
    }

    @Test
    void shouldLeaveBetaMarketOrderPendingWhenProductHasNoStock() {
        int shirtStockBefore = stockOf("CAM-002");
        String body = """
                {"order":{"external_id":"%s","customer":{"first_name":"Cliente","last_name":"Teste"},
                 "lines":[{"product_code":"CAM-002","amount":1,"unit_price_cents":5990},
                          {"product_code":"BON-001","amount":1,"unit_price_cents":6990}]}}
                """.formatted(uniqueId("BM"));

        ResponseEntity<Map> response = rest.postForEntity("/api/webhooks/betamarket/orders", json(body), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("code", "ORDER_PENDING_STOCK");
        assertThat(orderOf(response)).containsEntry("status", "PENDING_STOCK").containsEntry("totalAmount", 129.80);
        assertThat(stockOf("CAM-002")).isEqualTo(shirtStockBefore);
        assertThat(stockOf("BON-001")).isZero();
    }

    @Test
    void shouldReturnOriginalOrderWithoutDecreasingStockWhenSameOrderIsSentAgain() {
        String body = alfaShopOrder(uniqueId("AS"), "TEN-001");
        ResponseEntity<Map> first = rest.postForEntity("/api/webhooks/alfashop/orders", json(body), Map.class);
        int stockAfterFirst = stockOf("TEN-001");

        ResponseEntity<Map> resent = rest.postForEntity("/api/webhooks/alfashop/orders", json(body), Map.class);

        assertThat(resent.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resent.getBody())
                .containsEntry("code", "ORDER_ALREADY_RECEIVED")
                .extractingByKey("message").asString().contains("já tinha sido recebido");
        assertThat(orderOf(resent)).containsEntry("id", orderOf(first).get("id"));
        assertThat(resent.getHeaders().getLocation()).isNull();
        assertThat(stockOf("TEN-001")).isEqualTo(stockAfterFirst);
    }

    @Test
    void shouldNeverSellMoreThanStockWhenOrdersForSameProductArriveTogether() throws Exception {
        String sku = createProductWithStock(1);
        List<String> bodies = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            bodies.add(alfaShopOrder(uniqueId("AS"), sku));
        }

        List<ResponseEntity<Map>> responses = postAllAtOnce(bodies);

        assertThat(responses).allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED));
        assertThat(responses).filteredOn(response -> "CONFIRMED".equals(orderOf(response).get("status"))).hasSize(1);
        assertThat(stockOf(sku)).isZero();
    }

    @Test
    void shouldSaveOrderOnceWhenSameOrderArrivesManyTimesTogether() throws Exception {
        String sku = createProductWithStock(10);
        String body = alfaShopOrder(uniqueId("AS"), sku);
        List<String> bodies = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            bodies.add(body);
        }

        List<ResponseEntity<Map>> responses = postAllAtOnce(bodies);

        assertThat(responses).filteredOn(response -> response.getStatusCode() == HttpStatus.CREATED).hasSize(1);
        assertThat(responses).filteredOn(response -> response.getStatusCode() == HttpStatus.OK)
                .hasSize(CONCURRENT_REQUESTS - 1);
        Object firstOrderId = orderOf(responses.get(0)).get("id");
        assertThat(responses).extracting(response -> orderOf(response).get("id")).containsOnly(firstOrderId);
        assertThat(stockOf(sku)).isEqualTo(9);
    }

    @Test
    void shouldReturnOrderDetailWithItems() {
        String externalId = uniqueId("AS");
        String body = """
                {"orderId":"%s","buyer":{"fullName":"Cliente"},"items":[{"sku":"FON-001","qty":1,"price":49.90}]}
                """.formatted(externalId);
        ResponseEntity<Map> created = rest.postForEntity("/api/webhooks/alfashop/orders", json(body), Map.class);

        Map<String, Object> detail = rest.getForObject("/api/orders/" + orderOf(created).get("id"), Map.class);

        assertThat(detail).containsEntry("externalOrderId", externalId);
        assertThat((List<?>) detail.get("items")).hasSize(1);
    }

    @Test
    void shouldFilterOrdersByStatus() {
        ResponseEntity<Map> response = rest.getForEntity("/api/orders?status=PENDING_STOCK&size=100", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).allSatisfy(order -> assertThat(order).containsEntry("status", "PENDING_STOCK"));
    }

    @Test
    void shouldReturnNotFoundForUnknownChannel() {
        ResponseEntity<Map> response = rest.postForEntity("/api/webhooks/gamashop/orders", json("{}"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .containsEntry("code", "CHANNEL_NOT_FOUND")
                .containsEntry("message", "O canal \"gamashop\" não está integrado à sua conta.");
    }

    @Test
    void shouldReturnBadRequestWithFieldsForInvalidPayload() {
        ResponseEntity<Map> response = rest.postForEntity("/api/webhooks/alfashop/orders",
                json("{\"orderId\":\"AS-X\",\"items\":[]}"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "INVALID_MARKETPLACE_PAYLOAD");
        assertThat(response.getBody().get("message").toString()).contains("AlfaShop");
        assertThat(response.getBody().get("errors").toString()).contains("buyer", "items");
    }

    @Test
    void shouldReturnUnprocessableForUnknownSku() {
        String body = """
                {"orderId":"%s","buyer":{"fullName":"Cliente"},"items":[{"sku":"XYZ-999","qty":1,"price":1.00}]}
                """.formatted(uniqueId("AS"));

        ResponseEntity<Map> response = rest.postForEntity("/api/webhooks/alfashop/orders", json(body), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsEntry("code", "UNKNOWN_SKU");
        assertThat(response.getBody().get("message").toString()).contains("XYZ-999");
    }

    @Test
    void shouldReturnNotFoundWithSellerMessageForUnknownOrder() {
        ResponseEntity<Map> response = rest.getForEntity("/api/orders/999999", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .containsEntry("code", "ORDER_NOT_FOUND")
                .containsEntry("message", "Não encontramos o pedido 999999.");
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
