package com.giovanni.orderhub.exception.integration;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.ErrorCode;
import com.giovanni.orderhub.exception.ApiException;

/** O canal passou do limite de requisições da janela atual. Vira HTTP 429 com o header {@code Retry-After}. */
public class RateLimitExceededException extends ApiException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(Channel channel, int limitPerMinute, long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED,
                "Canal %s passou do limite de %d requisições por minuto.".formatted(channel, limitPerMinute),
                channel.getDisplayName(), retryAfterSeconds);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
