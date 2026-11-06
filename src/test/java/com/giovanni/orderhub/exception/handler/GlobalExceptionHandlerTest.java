package com.giovanni.orderhub.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.giovanni.orderhub.exception.product.InsufficientStockException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnStandardMessageAndHideInternalDetailsForUnexpectedError() {
        ResponseEntity<ProblemDetail> response =
                handler.handleUnexpected(new NullPointerException("campo x era nulo na linha 42"));

        ProblemDetail body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.getProperties()).containsEntry("code", "INTERNAL_ERROR");
        assertThat(body.getProperties().get("message").toString()).startsWith("Não foi possível concluir agora");
        assertThat(body.getDetail()).doesNotContain("linha 42");
    }

    @Test
    void shouldSeparateSellerMessageFromTechnicalDetail() {
        ResponseEntity<ProblemDetail> response =
                handler.handleApiException(new InsufficientStockException("CAM-001", 2, -5));

        ProblemDetail body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(body.getProperties())
                .containsEntry("code", "INSUFFICIENT_STOCK")
                .containsEntry("message", "O produto CAM-001 tem só 2 unidade(s) em estoque.");
        assertThat(body.getDetail()).contains("alteração pedida -5");
    }
}
