package com.giovanni.orderhub.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class AppConfig {

    /** Relógio injetável: em produção usa o do sistema; nos testes, um relógio controlado. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
