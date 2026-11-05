package com.giovanni.orderhub.shared.error;

/** Requisição válida, mas que viola uma regra de negócio. Vira HTTP 422. */
public abstract class BusinessRuleException extends RuntimeException {

    protected BusinessRuleException(String message) {
        super(message);
    }
}
