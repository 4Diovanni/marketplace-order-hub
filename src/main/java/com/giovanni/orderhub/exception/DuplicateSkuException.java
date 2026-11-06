package com.giovanni.orderhub.exception;

public class DuplicateSkuException extends ConflictException {

    public DuplicateSkuException(String sku) {
        super("Já existe um produto com SKU '%s'.".formatted(sku));
    }
}
