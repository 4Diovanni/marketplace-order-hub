package com.giovanni.orderhub.integration.betamarket;

import com.fasterxml.jackson.databind.JsonNode;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.integration.OrderPayloadMapper;
import com.giovanni.orderhub.integration.PayloadReader;
import com.giovanni.orderhub.model.IncomingOrder;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BetaMarketMapper implements OrderPayloadMapper {

    private static final int CENTS_SCALE = 2;

    private final PayloadReader payloadReader;

    public BetaMarketMapper(PayloadReader payloadReader) {
        this.payloadReader = payloadReader;
    }

    @Override
    public Channel channel() {
        return Channel.BETAMARKET;
    }

    @Override
    public IncomingOrder toIncomingOrder(JsonNode json) {
        BetaMarketOrderPayload.OrderData order = payloadReader.read(channel(), json, BetaMarketOrderPayload.class).order();
        return new IncomingOrder(
                Channel.BETAMARKET,
                order.externalId(),
                fullName(order.customer()),
                order.lines().stream()
                        .map(line -> new IncomingOrder.Item(
                                line.productCode(), line.amount(), centsToReais(line.unitPriceCents())))
                        .toList());
    }

    /** 5990 cents → 59.90 */
    static BigDecimal centsToReais(long cents) {
        return BigDecimal.valueOf(cents, CENTS_SCALE);
    }

    private static String fullName(BetaMarketOrderPayload.Customer customer) {
        String lastName = customer.lastName() == null ? "" : customer.lastName();
        return (customer.firstName() + " " + lastName).trim();
    }
}
