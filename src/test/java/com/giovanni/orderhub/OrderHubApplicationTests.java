package com.giovanni.orderhub;

import static org.assertj.core.api.Assertions.assertThat;

import com.giovanni.orderhub.support.MySqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderHubApplicationTests extends MySqlContainerSupport {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointIsUp() {
        String body = restTemplate.getForObject("/actuator/health", String.class);

        assertThat(body).contains("\"status\":\"UP\"");
    }
}
