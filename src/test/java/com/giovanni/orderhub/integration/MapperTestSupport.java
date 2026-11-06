package com.giovanni.orderhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/** Monta um {@link PayloadReader} real (Jackson + Bean Validation) sem subir o Spring. */
public final class MapperTestSupport {

    /** Mesma configuração padrão do Spring (ex.: ignora campos desconhecidos). */
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

    /** Lê um dos payloads de exemplo documentados em {@code docs/exemplos}. */
    public static JsonNode example(String fileName) {
        try {
            return OBJECT_MAPPER.readTree(Path.of("docs/exemplos", fileName).toFile());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
