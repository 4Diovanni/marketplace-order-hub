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
 * Limita quantas requisições cada marketplace pode fazer por minuto (janela fixa de 1 minuto).
 *
 * <p>O estado fica em memória, o que vale para uma instância da aplicação.
 * Com várias instâncias, o contador teria que ir para um lugar compartilhado (ex.: Redis).
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

    /** @throws RateLimitExceededException se o canal já usou todas as requisições da janela atual. */
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

    /** Arredonda para cima e nunca devolve zero: o marketplace espera um número inteiro de segundos no Retry-After. */
    private static long secondsUntilWindowEnds(Window window, Instant now) {
        long millis = Duration.between(now, window.end()).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }

    /** Falha na inicialização se algum canal ficou sem limite configurado, em vez de descobrir em produção. */
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
