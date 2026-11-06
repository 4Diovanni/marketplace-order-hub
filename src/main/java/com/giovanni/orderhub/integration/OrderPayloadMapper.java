package com.giovanni.orderhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.exception.integration.InvalidPayloadException;
import com.giovanni.orderhub.model.IncomingOrder;

/**
 * Converte o JSON de um marketplace para o modelo interno (Strategy).
 * Para integrar um marketplace novo basta criar outra implementação anotada com {@code @Component}.
 */
public interface OrderPayloadMapper {

    Channel channel();

    /** @throws InvalidPayloadException se o payload não tem o formato esperado. */
    IncomingOrder toIncomingOrder(JsonNode payload);
}
