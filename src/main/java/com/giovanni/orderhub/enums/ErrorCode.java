package com.giovanni.orderhub.enums;

import org.springframework.http.HttpStatus;

/**
 * Catálogo de erros da API, num lugar só.
 *
 * <p>Cada código tem o status HTTP, um título curto e a mensagem que o seller vê no front.
 * O {@code code} é estável: o front pode usá-lo para decidir o que mostrar sem depender do texto.
 */
public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dados inválidos",
            "Alguns campos estão inválidos. Confira os dados e tente de novo."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Requisição ilegível",
            "Não conseguimos ler os dados enviados. Confira se o formato está correto."),
    INVALID_MARKETPLACE_PAYLOAD(HttpStatus.BAD_REQUEST, "Pedido do marketplace fora do formato",
            "O pedido da %s chegou incompleto ou fora do formato esperado e não foi importado."),

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Produto não encontrado",
            "Não encontramos o produto com SKU %s no seu catálogo."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Pedido não encontrado",
            "Não encontramos o pedido %s."),
    CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "Canal não integrado",
            "O canal \"%s\" não está integrado à sua conta."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Endereço inexistente",
            "O endereço acessado não existe."),

    DUPLICATE_SKU(HttpStatus.CONFLICT, "SKU duplicado",
            "Já existe um produto com o SKU %s no seu catálogo."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "Conflito de dados",
            "Esta operação conflita com dados já cadastrados. Atualize a página e tente de novo."),

    INSUFFICIENT_STOCK(HttpStatus.UNPROCESSABLE_ENTITY, "Estoque insuficiente",
            "O produto %s tem só %d unidade(s) em estoque."),
    UNKNOWN_SKU(HttpStatus.UNPROCESSABLE_ENTITY, "Produto fora do catálogo",
            "O pedido tem produtos que não estão no seu catálogo: %s. Cadastre os SKUs e peça o reenvio do pedido."),

    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Limite de requisições atingido",
            "A %s enviou muitos pedidos em pouco tempo. Eles voltam a ser aceitos em %d segundo(s)."),

    REQUEST_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "Operação não suportada",
            "Esta operação não é suportada."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado",
            "Não foi possível concluir agora. Tente de novo em alguns minutos e, se continuar, fale com o suporte.");

    private final HttpStatus status;
    private final String title;
    private final String messageTemplate;

    ErrorCode(HttpStatus status, String title, String messageTemplate) {
        this.status = status;
        this.title = title;
        this.messageTemplate = messageTemplate;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    /** Preenche a mensagem para o seller com os valores do caso (SKU, canal...). */
    public String formatMessage(Object... args) {
        return messageTemplate.formatted(args);
    }
}
