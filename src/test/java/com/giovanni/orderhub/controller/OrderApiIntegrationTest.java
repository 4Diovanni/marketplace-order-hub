package com.giovanni.orderhub.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.giovanni.orderhub.support.MySqlContainerSupport;
import java.util.List;
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

/** End-to-end order flow: webhook → mapper → stock rule → MySQL → query. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiIntegrationTest extends MySqlContainerSupport {

    @Autowired
    private TestRestTemplate rest;

    private int stockOf(String sku) {
        return (Integer) rest.getForObject("/api/products/" + sku, Map.class).get("stockQuantity");
    }

    private static String uniqueId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
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
