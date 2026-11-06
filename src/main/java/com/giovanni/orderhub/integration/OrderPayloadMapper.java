package com.giovanni.orderhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.exception.integration.InvalidPayloadException;
import com.giovanni.orderhub.model.IncomingOrder;

/**
 * Converts a marketplace's JSON into the internal model (Strategy).
 * To integrate a new marketplace, create another implementation annotated with {@code @Component}.
 */
public interface OrderPayloadMapper {

    Channel channel();

    /** @throws InvalidPayloadException if the payload does not have the expected format. */
    IncomingOrder toIncomingOrder(JsonNode payload);
}
