package com.giovanni.orderhub.service;

import com.giovanni.orderhub.config.RateLimitProperties;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.exception.integration.RateLimitExceededException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Limits how many requests each marketplace can make per minute (fixed 1-minute window).
 *
 * <p>State is kept in memory, which holds for a single application instance.
 * With several instances, the counter would have to move to shared storage (e.g. Redis).
 */
@Service
public class RateLimitService {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<Channel, Integer> limitByChannel;
    private final Clock clock;
    private final Map<Channel, Window> windowByChannel = new EnumMap<>(Channel.class);

    public RateLimitService(RateLimitProperties properties, Clock clock) {
        this.limitByChannel = properties.requestsPerMinute();
        this.clock = clock;
        requireLimitForEveryChannel();
    }

    /** @throws RateLimitExceededException if the channel already used every request of the current window. */
    public synchronized void checkLimit(Channel channel) {
        Instant now = clock.instant();
        Window window = currentWindow(channel, now);
        int limit = limitByChannel.get(channel);

        if (window.requestCount >= limit) {
            throw new RateLimitExceededException(channel, limit, secondsUntilWindowEnds(window, now));
        }
        window.requestCount++;
    }

    private Window currentWindow(Channel channel, Instant now) {
        Window window = windowByChannel.get(channel);
        boolean windowExpired = window == null || !now.isBefore(window.end());
        if (windowExpired) {
            window = new Window(now);
            windowByChannel.put(channel, window);
        }
        return window;
    }

    /** Rounds up and never returns zero: the marketplace expects a whole number of seconds in Retry-After. */
    private static long secondsUntilWindowEnds(Window window, Instant now) {
        long millis = Duration.between(now, window.end()).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }

    /** Fails at startup if any channel has no configured limit, instead of finding out in production. */
    private void requireLimitForEveryChannel() {
        for (Channel channel : Channel.values()) {
            Integer limit = limitByChannel.get(channel);
            if (limit == null || limit <= 0) {
                throw new IllegalStateException(
                        "Configure orderhub.rate-limit.requests-per-minute." + channel.getSlug() + " com um valor > 0");
            }
        }
    }

    private static final class Window {

        private final Instant start;
        private int requestCount;

        private Window(Instant start) {
            this.start = start;
        }

        private Instant end() {
            return start.plus(WINDOW);
        }
    }
}
