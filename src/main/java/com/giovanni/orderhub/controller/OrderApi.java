package com.giovanni.orderhub.controller;

import com.giovanni.orderhub.dto.order.OrderResponse;
import com.giovanni.orderhub.dto.order.OrderSummaryResponse;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** OpenAPI documentation of {@link OrderController}, kept here so the controller stays short. */
@Tag(name = "Pedidos", description = "Consulta dos pedidos já recebidos, no modelo interno do hub")
interface OrderApi {

    @Operation(summary = "Lista os pedidos",
            description = "Do mais recente para o mais antigo. Filtros opcionais; aceita page e size.")
    @ApiResponse(responseCode = "200", description = "Página de pedidos, sem os itens")
    Page<OrderSummaryResponse> search(
            @Parameter(description = "Só pedidos com este status") OrderStatus status,
            @Parameter(description = "Só pedidos deste marketplace") Channel channel,
            @Parameter(hidden = true) Pageable pageable);

    @Operation(summary = "Detalhe de um pedido, com os itens")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "404", description = "Pedido não existe (ORDER_NOT_FOUND)")
    OrderResponse findById(@Parameter(example = "1") Long id);
}
