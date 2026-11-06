package com.giovanni.orderhub.controller;

import com.giovanni.orderhub.dto.order.OrderResponse;
import com.giovanni.orderhub.dto.order.OrderSummaryResponse;
import com.giovanni.orderhub.model.Channel;
import com.giovanni.orderhub.model.OrderStatus;
import com.giovanni.orderhub.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Page<OrderSummaryResponse> search(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Channel channel,
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return orderService.search(status, channel, pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return orderService.findById(id);
    }
}
