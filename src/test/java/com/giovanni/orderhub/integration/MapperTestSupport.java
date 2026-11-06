package com.giovanni.orderhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/** Builds a real {@link PayloadReader} (Jackson + Bean Validation) without starting Spring. */
public final class MapperTestSupport {

    /** Same defaults as Spring (e.g. ignores unknown fields). */
    private static final ObjectMapper OBJECT_MAPPER = Jackson2ObjectMapperBuilder.json().build();

    private MapperTestSupport() {
    }

    public static PayloadReader payloadReader() {
        return new PayloadReader(OBJECT_MAPPER, Validation.buildDefaultValidatorFactory().getValidator());
    }

    public static JsonNode json(String content) {
        try {
            return OBJECT_MAPPER.readTree(content);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** Reads one of the sample payloads documented in {@code docs/exemplos}. */
    public static JsonNode example(String fileName) {
        try {
            return OBJECT_MAPPER.readTree(Path.of("docs/exemplos", fileName).toFile());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
