package com.giovanni.orderhub.exception;

/** Um campo inválido e o motivo, usado na lista {@code errors} das respostas 400. */
public record FieldViolation(String field, String message) {
}
