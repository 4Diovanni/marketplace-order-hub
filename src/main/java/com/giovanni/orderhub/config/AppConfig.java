package com.giovanni.orderhub.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class AppConfig {

    /** Injectable clock: the system clock in production, a controlled clock in tests. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
