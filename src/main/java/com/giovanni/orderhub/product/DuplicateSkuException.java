package com.giovanni.orderhub.product;

import com.giovanni.orderhub.shared.error.ConflictException;

public class DuplicateSkuException extends ConflictException {

    public DuplicateSkuException(String sku) {
        super("Já existe um produto com SKU '%s'.".formatted(sku));
    }
}
