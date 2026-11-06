package com.giovanni.orderhub.order;

import com.giovanni.orderhub.shared.error.NotFoundException;

public class UnknownChannelException extends NotFoundException {

    public UnknownChannelException(String slug) {
        super("Canal '%s' não é integrado ao hub.".formatted(slug));
    }
}
