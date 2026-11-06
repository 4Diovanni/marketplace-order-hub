package com.giovanni.orderhub.product;

import com.giovanni.orderhub.shared.error.NotFoundException;

public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException(String sku) {
        super("Produto com SKU '%s' não encontrado.".formatted(sku));
    }
}
