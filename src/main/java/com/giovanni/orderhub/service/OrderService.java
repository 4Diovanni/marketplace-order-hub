package com.giovanni.orderhub.service;

import static java.util.stream.Collectors.toMap;

import com.giovanni.orderhub.dto.order.OrderResponse;
import com.giovanni.orderhub.dto.order.OrderSummaryResponse;
import com.giovanni.orderhub.exception.OrderNotFoundException;
import com.giovanni.orderhub.exception.UnknownSkuException;
import com.giovanni.orderhub.model.Channel;
import com.giovanni.orderhub.model.IncomingOrder;
import com.giovanni.orderhub.model.Order;
import com.giovanni.orderhub.model.OrderStatus;
import com.giovanni.orderhub.model.Product;
import com.giovanni.orderhub.repository.OrderRepository;
import com.giovanni.orderhub.repository.ProductRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Registra um pedido aplicando a regra tudo-ou-nada:
     * se todos os itens têm estoque, baixa o estoque e confirma; se algum falta, não baixa nada e deixa pendente.
     *
     * @throws UnknownSkuException se algum SKU do pedido não existe no cadastro (nada é gravado).
     */
    @Transactional
    public OrderResponse receive(IncomingOrder incoming) {
        Map<String, Integer> quantityBySku = sumQuantitiesBySku(incoming.items());
        Map<String, Product> productsBySku = loadProducts(quantityBySku.keySet());

        boolean hasStockForAllItems = quantityBySku.entrySet().stream()
                .allMatch(entry -> productsBySku.get(entry.getKey()).hasStockFor(entry.getValue()));

        OrderStatus status;
        if (hasStockForAllItems) {
            quantityBySku.forEach((sku, quantity) -> productsBySku.get(sku).adjustStock(-quantity));
            status = OrderStatus.CONFIRMED;
        } else {
            status = OrderStatus.PENDING_STOCK;
        }

        Order saved = orderRepository.saveAndFlush(Order.of(incoming, status));
        log.info("Pedido recebido: channel={}, externalOrderId={}, status={}, orderId={}",
                saved.getChannel(), saved.getExternalOrderId(), saved.getStatus(), saved.getId());
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> search(OrderStatus status, Channel channel, Pageable pageable) {
        return orderRepository.search(status, channel, pageable).map(OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return orderRepository.findWithItemsById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /** O mesmo SKU pode vir em mais de uma linha do pedido; a checagem de estoque precisa do total. */
    private static Map<String, Integer> sumQuantitiesBySku(List<IncomingOrder.Item> items) {
        return items.stream().collect(toMap(
                IncomingOrder.Item::sku, IncomingOrder.Item::quantity, Integer::sum, LinkedHashMap::new));
    }

    private Map<String, Product> loadProducts(Set<String> skus) {
        Map<String, Product> productsBySku = productRepository.findAllBySkuIn(skus).stream()
                .collect(toMap(Product::getSku, Function.identity()));

        List<String> unknownSkus = skus.stream().filter(sku -> !productsBySku.containsKey(sku)).toList();
        if (!unknownSkus.isEmpty()) {
            throw new UnknownSkuException(unknownSkus);
        }
        return productsBySku;
    }
}
