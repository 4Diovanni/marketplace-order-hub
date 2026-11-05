package com.giovanni.orderhub.product;

import com.giovanni.orderhub.shared.error.BusinessRuleException;

public class InsufficientStockException extends BusinessRuleException {

    public InsufficientStockException(String sku, int available, int requestedChange) {
        super("Estoque insuficiente para o SKU '%s': disponível %d, alteração pedida %d."
                .formatted(sku, available, requestedChange));
    }
}
