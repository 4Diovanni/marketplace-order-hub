package com.giovanni.orderhub.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.giovanni.orderhub.support.MySqlContainerSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Contexto próprio com limite baixo na BetaMarket, para provocar o 429 sem afetar os outros testes. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "orderhub.rate-limit.requests-per-minute.betamarket=2")
class RateLimitIntegrationTest extends MySqlContainerSupport {

    private static final String WEBHOOK = "/api/webhooks/betamarket/orders";

    @Autowired
    private TestRestTemplate rest;

    @Test
    void shouldReturnTooManyRequestsWithRetryAfterWhenChannelExceedsLimit() {
        rest.postForEntity(WEBHOOK, emptyJson(), Map.class);
        rest.postForEntity(WEBHOOK, emptyJson(), Map.class);

        ResponseEntity<Map> response = rest.postForEntity(WEBHOOK, emptyJson(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank();
        assertThat(response.getBody())
                .containsEntry("code", "RATE_LIMIT_EXCEEDED")
                .containsKey("retryAfterSeconds");
        assertThat(response.getBody().get("message").toString()).contains("BetaMarket");
    }

    private static HttpEntity<String> emptyJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>("{}", headers);
    }
}
