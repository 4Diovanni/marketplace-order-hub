package com.giovanni.orderhub.dto.error;

/** Um campo inválido e o motivo, usado na lista {@code errors} das respostas 400. */
public record FieldViolation(String field, String message) {
}
