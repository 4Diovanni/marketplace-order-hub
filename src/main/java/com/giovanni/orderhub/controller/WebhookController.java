package com.giovanni.orderhub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.giovanni.orderhub.dto.order.OrderReceipt;
import com.giovanni.orderhub.dto.order.WebhookOrderResponse;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.integration.OrderPayloadMappers;
import com.giovanni.orderhub.model.IncomingOrder;
import com.giovanni.orderhub.service.OrderService;
import com.giovanni.orderhub.service.RateLimitService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Entry point for marketplaces: each channel sends the order in its own format to the same route. */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController implements WebhookApi {

    private final RateLimitService rateLimitService;
    private final OrderPayloadMappers mappers;
    private final OrderService orderService;

    public WebhookController(RateLimitService rateLimitService, OrderPayloadMappers mappers,
                             OrderService orderService) {
        this.rateLimitService = rateLimitService;
        this.mappers = mappers;
        this.orderService = orderService;
    }

    @Override
    @PostMapping("/{channel}/orders")
    public ResponseEntity<WebhookOrderResponse> receiveOrder(@PathVariable("channel") String channelSlug,
                                                             @RequestBody JsonNode payload) {
        Channel channel = Channel.fromSlug(channelSlug);
        rateLimitService.checkLimit(channel);

        IncomingOrder incoming = mappers.forChannel(channel).toIncomingOrder(payload);
        OrderReceipt receipt = orderService.receive(incoming);
        if (receipt.isDuplicate()) {
            return ResponseEntity.ok(WebhookOrderResponse.alreadyReceived(receipt.order()));
        }

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/orders/{id}")
                .buildAndExpand(receipt.order().id())
                .toUri();
        return ResponseEntity.created(location).body(WebhookOrderResponse.from(receipt.order()));
    }
}
