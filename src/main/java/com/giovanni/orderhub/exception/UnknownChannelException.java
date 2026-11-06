package com.giovanni.orderhub.exception;

public class UnknownChannelException extends NotFoundException {

    public UnknownChannelException(String slug) {
        super("Canal '%s' não é integrado ao hub.".formatted(slug));
    }
}
