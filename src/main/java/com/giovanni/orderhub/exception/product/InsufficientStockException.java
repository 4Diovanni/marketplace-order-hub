package com.giovanni.orderhub.exception.product;

import com.giovanni.orderhub.enums.ErrorCode;
import com.giovanni.orderhub.exception.ApiException;

public class InsufficientStockException extends ApiException {

    public InsufficientStockException(String sku, int available, int requestedChange) {
        super(ErrorCode.INSUFFICIENT_STOCK,
                "Estoque insuficiente para o SKU '%s': disponível %d, alteração pedida %d."
                        .formatted(sku, available, requestedChange),
                sku, available);
    }
}
