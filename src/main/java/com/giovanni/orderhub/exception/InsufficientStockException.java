package com.giovanni.orderhub.exception;

public class InsufficientStockException extends BusinessRuleException {

    public InsufficientStockException(String sku, int available, int requestedChange) {
        super("Estoque insuficiente para o SKU '%s': disponível %d, alteração pedida %d."
                .formatted(sku, available, requestedChange));
    }
}
