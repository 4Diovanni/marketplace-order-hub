package com.giovanni.orderhub.exception.integration;

import com.giovanni.orderhub.enums.ErrorCode;
import com.giovanni.orderhub.exception.ApiException;

public class UnknownChannelException extends ApiException {

    public UnknownChannelException(String slug) {
        super(ErrorCode.CHANNEL_NOT_FOUND, "Canal '%s' não é integrado ao hub.".formatted(slug), slug);
    }
}
