package com.giovanni.orderhub.config;

import com.giovanni.orderhub.enums.Channel;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Requests-per-minute limit of each marketplace, read from {@code orderhub.rate-limit} in application.yml.
 * Each channel has its own, because each API contract is different (see docs/marketplaces.md).
 */
@ConfigurationProperties(prefix = "orderhub.rate-limit")
public record RateLimitProperties(Map<Channel, Integer> requestsPerMinute) {

    public RateLimitProperties {
        requestsPerMinute = requestsPerMinute == null ? Map.of() : Map.copyOf(requestsPerMinute);
    }
}
