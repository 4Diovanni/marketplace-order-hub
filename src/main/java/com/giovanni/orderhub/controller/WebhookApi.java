package com.giovanni.orderhub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.giovanni.orderhub.dto.order.WebhookOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

/** OpenAPI documentation of {@link WebhookController}, kept here so the controller stays short. */
@Tag(name = "Webhooks", description = "Entrada dos pedidos dos marketplaces, cada um no seu formato")
interface WebhookApi {

    String ALFASHOP_EXAMPLE = """
            {"orderId":"AS-10001","buyer":{"fullName":"Cliente Teste"},
             "items":[{"sku":"CAM-001","qty":2,"price":59.90},{"sku":"FON-001","qty":1,"price":49.90}]}""";

    String BETAMARKET_EXAMPLE = """
            {"order":{"external_id":"BM-778","customer":{"first_name":"Cliente","last_name":"Teste"},
             "lines":[{"product_code":"CAM-001","amount":1,"unit_price_cents":5990}]}}""";

    @Operation(summary = "Recebe um pedido de um marketplace", description = """
            O formato do corpo depende do canal (veja os exemplos e o docs/marketplaces.md). \
            Regra de estoque tudo ou nada: com estoque para todos os itens, baixa e confirma; \
            faltando qualquer um, não baixa nada e deixa pendente. \
            O mesmo pedido (canal + id externo) enviado de novo devolve 200 com o original.""")
    @ApiResponse(responseCode = "201", description = "Pedido novo: ORDER_CONFIRMED ou ORDER_PENDING_STOCK")
    @ApiResponse(responseCode = "200", description = "Pedido já recebido antes: ORDER_ALREADY_RECEIVED, nada muda")
    @ApiResponse(responseCode = "400", description = "Corpo fora do contrato do canal (INVALID_MARKETPLACE_PAYLOAD)")
    @ApiResponse(responseCode = "404", description = "Canal não integrado (CHANNEL_NOT_FOUND)")
    @ApiResponse(responseCode = "422", description = "SKU fora do catálogo (UNKNOWN_SKU)")
    @ApiResponse(responseCode = "429", description = "Limite de requisições do canal (RATE_LIMIT_EXCEEDED), "
            + "com header Retry-After")
    @ApiResponse(responseCode = "503", description = "Estoque disputado por outro pedido mesmo após 3 tentativas "
            + "(CONCURRENT_STOCK_UPDATE); o marketplace reenvia")
    ResponseEntity<WebhookOrderResponse> receiveOrder(
            @Parameter(description = "Marketplace de origem",
                    schema = @Schema(allowableValues = {"alfashop", "betamarket"}))
            String channelSlug,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(name = "alfashop", summary = "AlfaShop: plano, reais", value = ALFASHOP_EXAMPLE),
                        @ExampleObject(name = "betamarket", summary = "BetaMarket: aninhado, centavos",
                                value = BETAMARKET_EXAMPLE)
                    }))
            JsonNode payload);
}
