package com.giovanni.orderhub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.giovanni.orderhub.dto.order.OrderResponse;
import com.giovanni.orderhub.integration.OrderPayloadMappers;
import com.giovanni.orderhub.model.Channel;
import com.giovanni.orderhub.model.IncomingOrder;
import com.giovanni.orderhub.service.OrderService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Ponto de entrada dos marketplaces: cada canal manda o pedido no próprio formato para a mesma rota. */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final OrderPayloadMappers mappers;
    private final OrderService orderService;

    public WebhookController(OrderPayloadMappers mappers, OrderService orderService) {
        this.mappers = mappers;
        this.orderService = orderService;
    }

    @PostMapping("/{channel}/orders")
    public ResponseEntity<OrderResponse> receiveOrder(@PathVariable("channel") String channelSlug,
                                                      @RequestBody JsonNode payload) {
        Channel channel = Channel.fromSlug(channelSlug);
        IncomingOrder incoming = mappers.forChannel(channel).toIncomingOrder(payload);
        OrderResponse order = orderService.receive(incoming);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/orders/{id}")
                .buildAndExpand(order.id())
                .toUri();
        return ResponseEntity.created(location).body(order);
    }
}
