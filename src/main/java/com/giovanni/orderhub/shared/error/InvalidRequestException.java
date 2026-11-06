package com.giovanni.orderhub.shared.error;

import java.util.List;

/** Entrada inválida detectada fora do Bean Validation automático do Spring (ex.: payload de marketplace). Vira HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    private final List<FieldViolation> violations;

    public InvalidRequestException(String message, List<FieldViolation> violations) {
        super(message);
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }
}
