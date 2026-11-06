package com.giovanni.orderhub.integration;

import com.giovanni.orderhub.model.Channel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Registro dos mappers por canal, montado a partir de todos os {@link OrderPayloadMapper} do contexto. */
@Component
public class OrderPayloadMappers {

    private final Map<Channel, OrderPayloadMapper> mappersByChannel = new EnumMap<>(Channel.class);

    public OrderPayloadMappers(List<OrderPayloadMapper> mappers) {
        mappers.forEach(mapper -> {
            OrderPayloadMapper previous = mappersByChannel.put(mapper.channel(), mapper);
            if (previous != null) {
                throw new IllegalStateException("Mais de um mapper para o canal " + mapper.channel());
            }
        });
    }

    public OrderPayloadMapper forChannel(Channel channel) {
        OrderPayloadMapper mapper = mappersByChannel.get(channel);
        if (mapper == null) {
            throw new IllegalStateException("Canal " + channel + " não tem mapper configurado.");
        }
        return mapper;
    }
}
