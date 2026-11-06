package com.giovanni.orderhub.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.giovanni.orderhub.support.MySqlContainerSupport;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Testa a API de produtos de ponta a ponta: HTTP → controller → service → MySQL real (Flyway incluso). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiIntegrationTest extends MySqlContainerSupport {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void shouldServeSeededProducts() {
        ResponseEntity<Map> response = rest.getForEntity("/api/products/CAM-001", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("name", "Camiseta básica preta");
    }

    @Test
    void shouldCreateUpdateAdjustAndDeleteProduct() {
        Map<String, Object> create = Map.of("sku", "TST-100", "name", "Produto teste",
                "price", new BigDecimal("10.00"), "stockQuantity", 5);
        ResponseEntity<Map> created = rest.postForEntity("/api/products", create, Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).hasPath("/api/products/TST-100");

        Map<String, Object> update = Map.of("name", "Produto teste v2", "price", new BigDecimal("12.50"));
        ResponseEntity<Map> updated = rest.exchange("/api/products/TST-100", HttpMethod.PUT,
                new HttpEntity<>(update), Map.class);
        assertThat(updated.getBody()).containsEntry("name", "Produto teste v2");

        ResponseEntity<Map> adjusted = rest.exchange("/api/products/TST-100/stock", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("delta", -5)), Map.class);
        assertThat(adjusted.getBody()).containsEntry("stockQuantity", 0);

        ResponseEntity<Void> deleted = rest.exchange("/api/products/TST-100", HttpMethod.DELETE, null, Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(rest.getForEntity("/api/products/TST-100", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnConflictForDuplicateSku() {
        Map<String, Object> create = Map.of("sku", "CAM-001", "name", "Duplicado",
                "price", new BigDecimal("10.00"), "stockQuantity", 1);

        ResponseEntity<Map> response = rest.postForEntity("/api/products", create, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("title", "Conflito");
    }

    @Test
    void shouldReturnUnprocessableWhenStockWouldBeNegative() {
        ResponseEntity<Map> response = rest.exchange("/api/products/BON-001/stock", HttpMethod.PATCH,
                new HttpEntity<>(Map.of("delta", -1)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(rest.getForEntity("/api/products/BON-001", Map.class).getBody())
                .containsEntry("stockQuantity", 0);
    }

    @Test
    void shouldListFieldErrorsForInvalidPayload() {
        Map<String, Object> invalid = Map.of("sku", "minusculo", "name", "", "price", -1, "stockQuantity", -1);

        ResponseEntity<Map> response = rest.postForEntity("/api/products", invalid, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("errors").toString())
                .contains("sku", "name", "price", "stockQuantity");
    }

    @Test
    void shouldPaginateProductsSortedBySku() {
        ResponseEntity<Map> response = rest.getForEntity("/api/products?size=3", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("content", "page");
        assertThat((java.util.List<?>) response.getBody().get("content")).hasSize(3);
    }
}
