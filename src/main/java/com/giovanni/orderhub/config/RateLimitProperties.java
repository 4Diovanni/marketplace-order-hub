package com.giovanni.orderhub.config;

import com.giovanni.orderhub.enums.Channel;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limite de requisições por minuto de cada marketplace, lido de {@code orderhub.rate-limit} no application.yml.
 * Cada canal tem o seu, porque cada contrato de API é diferente (ver docs/marketplaces.md).
 */
@ConfigurationProperties(prefix = "orderhub.rate-limit")
public record RateLimitProperties(Map<Channel, Integer> requestsPerMinute) {

    public RateLimitProperties {
        requestsPerMinute = requestsPerMinute == null ? Map.of() : Map.copyOf(requestsPerMinute);
    }
}
