package com.giovanni.orderhub.shared.error;

/** Conflito com o estado atual (ex.: chave duplicada). Vira HTTP 409. */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}
