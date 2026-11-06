package com.giovanni.orderhub.shared.error;

/** Recurso não encontrado. Vira HTTP 404. */
public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
