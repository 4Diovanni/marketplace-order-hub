package com.giovanni.orderhub.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.giovanni.orderhub.support.MySqlContainerSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** The generated OpenAPI document must describe every endpoint, its error codes and the marketplace examples. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocsIntegrationTest extends MySqlContainerSupport {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @SuppressWarnings("unchecked")
    void shouldDocumentEveryEndpoint() {
        Map<String, Object> docs = rest.getForObject("/v3/api-docs", Map.class);

        assertThat((Map<String, Object>) docs.get("paths")).containsOnlyKeys(
                "/api/products", "/api/products/{sku}", "/api/products/{sku}/stock",
                "/api/orders", "/api/orders/{id}", "/api/webhooks/{channel}/orders");
    }

    @Test
    void shouldDocumentWebhookResponsesAndMarketplaceExamples() {
        ResponseEntity<String> response = rest.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("ORDER_ALREADY_RECEIVED", "RATE_LIMIT_EXCEEDED", "CONCURRENT_STOCK_UPDATE")
                .contains("\"alfashop\"", "\"betamarket\"", "unit_price_cents");
    }

    @Test
    void shouldServeSwaggerUi() {
        ResponseEntity<String> response = rest.getForEntity("/swagger-ui/index.html", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
