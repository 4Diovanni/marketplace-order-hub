package com.giovanni.orderhub.controller;

import com.giovanni.orderhub.dto.product.CreateProductRequest;
import com.giovanni.orderhub.dto.product.ProductResponse;
import com.giovanni.orderhub.dto.product.StockAdjustmentRequest;
import com.giovanni.orderhub.dto.product.UpdateProductRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

/** OpenAPI documentation of {@link ProductController}, kept here so the controller stays short. */
@Tag(name = "Produtos", description = "Catálogo do seller e ajuste de estoque")
interface ProductApi {

    @Operation(summary = "Cadastra um produto")
    @ApiResponse(responseCode = "201", description = "Produto criado; o header Location aponta para ele")
    @ApiResponse(responseCode = "400", description = "Campos inválidos (VALIDATION_ERROR), com a lista em errors")
    @ApiResponse(responseCode = "409", description = "Já existe um produto com esse SKU (DUPLICATE_SKU)")
    ResponseEntity<ProductResponse> create(CreateProductRequest request);

    @Operation(summary = "Lista os produtos", description = "Paginado e ordenado por SKU. Aceita page, size e sort.")
    @ApiResponse(responseCode = "200", description = "Página de produtos")
    Page<ProductResponse> findAll(@Parameter(hidden = true) Pageable pageable);

    @Operation(summary = "Busca um produto pelo SKU")
    @ApiResponse(responseCode = "200", description = "Produto encontrado")
    @ApiResponse(responseCode = "404", description = "SKU não cadastrado (PRODUCT_NOT_FOUND)")
    ProductResponse findBySku(@Parameter(example = "CAM-001") String sku);

    @Operation(summary = "Atualiza nome e preço", description = "SKU e estoque não mudam por aqui.")
    @ApiResponse(responseCode = "200", description = "Produto atualizado")
    @ApiResponse(responseCode = "400", description = "Campos inválidos (VALIDATION_ERROR)")
    @ApiResponse(responseCode = "404", description = "SKU não cadastrado (PRODUCT_NOT_FOUND)")
    ProductResponse update(@Parameter(example = "CAM-001") String sku, UpdateProductRequest request);

    @Operation(summary = "Ajusta o estoque",
            description = "delta positivo repõe, negativo retira. O estoque nunca fica negativo. "
                    + "Ao repor, confirma por ordem de chegada os pedidos PENDING_STOCK que esperavam esse SKU "
                    + "(tudo ou nada por pedido; o que ainda não couber continua pendente).")
    @ApiResponse(responseCode = "200", description = "Estoque ajustado; stockQuantity já desconta os pedidos "
            + "pendentes confirmados")
    @ApiResponse(responseCode = "404", description = "SKU não cadastrado (PRODUCT_NOT_FOUND)")
    @ApiResponse(responseCode = "422", description = "O estoque ficaria negativo (INSUFFICIENT_STOCK)")
    @ApiResponse(responseCode = "503", description = "Outra operação alterou o produto ao mesmo tempo "
            + "(CONCURRENT_STOCK_UPDATE); tente de novo")
    ProductResponse adjustStock(@Parameter(example = "CAM-001") String sku, StockAdjustmentRequest request);

    @Operation(summary = "Remove um produto")
    @ApiResponse(responseCode = "204", description = "Produto removido")
    @ApiResponse(responseCode = "404", description = "SKU não cadastrado (PRODUCT_NOT_FOUND)")
    ResponseEntity<Void> delete(@Parameter(example = "CAM-001") String sku);
}
