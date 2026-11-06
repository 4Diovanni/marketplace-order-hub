package com.giovanni.orderhub.dto.error;

/** An invalid field and the reason, used in the {@code errors} list of 400 responses. */
public record FieldViolation(String field, String message) {
}
