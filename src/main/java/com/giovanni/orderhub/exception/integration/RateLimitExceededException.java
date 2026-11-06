package com.giovanni.orderhub.exception.integration;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.ErrorCode;
import com.giovanni.orderhub.exception.ApiException;

/** The channel went over the request limit of the current window. Becomes HTTP 429 with {@code Retry-After}. */
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
