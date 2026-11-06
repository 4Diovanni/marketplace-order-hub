package com.giovanni.orderhub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giovanni.orderhub.config.RateLimitProperties;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.exception.integration.RateLimitExceededException;
import com.giovanni.orderhub.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    private MutableClock clock;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-06T12:00:00Z"));
        RateLimitProperties properties = new RateLimitProperties(Map.of(Channel.ALFASHOP, 2, Channel.BETAMARKET, 1));
        rateLimitService = new RateLimitService(properties, clock);
    }

    @Test
    void shouldAllowRequestsUpToTheChannelLimit() {
        assertThatCode(() -> {
            rateLimitService.checkLimit(Channel.ALFASHOP);
            rateLimitService.checkLimit(Channel.ALFASHOP);
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectRequestAboveLimitWithSecondsUntilWindowEnds() {
        rateLimitService.checkLimit(Channel.ALFASHOP);
        rateLimitService.checkLimit(Channel.ALFASHOP);
        clock.advance(Duration.ofSeconds(20));

        assertThatThrownBy(() -> rateLimitService.checkLimit(Channel.ALFASHOP))
                .isInstanceOfSatisfying(RateLimitExceededException.class, ex -> {
                    assertThat(ex.getRetryAfterSeconds()).isEqualTo(40);
                    assertThat(ex.getSellerMessage()).contains("AlfaShop", "40 segundo(s)");
                });
    }

    @Test
    void shouldCountEachChannelSeparately() {
        rateLimitService.checkLimit(Channel.BETAMARKET);

        assertThatCode(() -> rateLimitService.checkLimit(Channel.ALFASHOP)).doesNotThrowAnyException();
        assertThatThrownBy(() -> rateLimitService.checkLimit(Channel.BETAMARKET))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void shouldAcceptRequestsAgainWhenWindowEnds() {
        rateLimitService.checkLimit(Channel.BETAMARKET);
        clock.advance(Duration.ofMinutes(1));

        assertThatCode(() -> rateLimitService.checkLimit(Channel.BETAMARKET)).doesNotThrowAnyException();
    }

    @Test
    void shouldFailOnStartupWhenAChannelHasNoLimit() {
        RateLimitProperties incomplete = new RateLimitProperties(Map.of(Channel.ALFASHOP, 10));

        assertThatThrownBy(() -> new RateLimitService(incomplete, clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("betamarket");
    }
}
