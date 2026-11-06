package com.giovanni.orderhub.integration;

import com.giovanni.orderhub.enums.Channel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Registry of mappers by channel, built from every {@link OrderPayloadMapper} in the context. */
@Component
public class OrderPayloadMappers {

    private final Map<Channel, OrderPayloadMapper> mappersByChannel = new EnumMap<>(Channel.class);

    public OrderPayloadMappers(List<OrderPayloadMapper> mappers) {
        for (OrderPayloadMapper mapper : mappers) {
            if (mappersByChannel.containsKey(mapper.channel())) {
                throw new IllegalStateException("Mais de um mapper para o canal " + mapper.channel());
            }
            mappersByChannel.put(mapper.channel(), mapper);
        }
    }

    public OrderPayloadMapper forChannel(Channel channel) {
        OrderPayloadMapper mapper = mappersByChannel.get(channel);
        if (mapper == null) {
            throw new IllegalStateException("Canal " + channel + " não tem mapper configurado.");
        }
        return mapper;
    }
}
