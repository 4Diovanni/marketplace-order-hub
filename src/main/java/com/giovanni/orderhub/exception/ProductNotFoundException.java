package com.giovanni.orderhub.exception;

public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException(String sku) {
        super("Produto com SKU '%s' não encontrado.".formatted(sku));
    }
}
