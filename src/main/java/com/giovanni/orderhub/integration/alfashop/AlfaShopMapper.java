package com.giovanni.orderhub.integration.alfashop;

import com.fasterxml.jackson.databind.JsonNode;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.integration.OrderPayloadMapper;
import com.giovanni.orderhub.integration.PayloadReader;
import com.giovanni.orderhub.model.IncomingOrder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class AlfaShopMapper implements OrderPayloadMapper {

    private static final int PRICE_SCALE = 2;

    private final PayloadReader payloadReader;

    public AlfaShopMapper(PayloadReader payloadReader) {
        this.payloadReader = payloadReader;
    }

    @Override
    public Channel channel() {
        return Channel.ALFASHOP;
    }

    @Override
    public IncomingOrder toIncomingOrder(JsonNode json) {
        AlfaShopOrderPayload payload = payloadReader.read(channel(), json, AlfaShopOrderPayload.class);
        return new IncomingOrder(
                Channel.ALFASHOP,
                payload.orderId(),
                payload.buyer().fullName().trim(),
                payload.items().stream()
                        .map(item -> new IncomingOrder.Item(item.sku(), item.qty(), toMoney(item.price())))
                        .toList());
    }

    /** JSON drops the trailing zero (59.90 becomes 59.9); normalize to 2 decimals. @Digits rejects more than 2. */
    private static BigDecimal toMoney(BigDecimal value) {
        return value.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }
}
