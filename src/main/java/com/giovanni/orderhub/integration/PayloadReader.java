package com.giovanni.orderhub.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giovanni.orderhub.dto.error.FieldViolation;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.exception.integration.InvalidPayloadException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Converte o JSON bruto no record tipado de cada marketplace e aplica o Bean Validation desse record. */
@Component
public class PayloadReader {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PayloadReader(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public <T> T read(Channel channel, JsonNode payload, Class<T> type) {
        T value = convert(channel, payload, type);
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            List<FieldViolation> errors = violations.stream()
                    .map(violation -> new FieldViolation(violation.getPropertyPath().toString(), violation.getMessage()))
                    .sorted(Comparator.comparing(FieldViolation::field))
                    .toList();
            throw new InvalidPayloadException(channel, "O payload do pedido tem campos inválidos.", errors);
        }
        return value;
    }

    private <T> T convert(Channel channel, JsonNode payload, Class<T> type) {
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (JsonProcessingException ex) {
            throw new InvalidPayloadException(channel, "O payload do pedido não tem o formato esperado.",
                    List.of(new FieldViolation("payload", ex.getOriginalMessage())));
        }
    }
}
