package com.giovanni.orderhub.exception.product;

import com.giovanni.orderhub.enums.ErrorCode;
import com.giovanni.orderhub.exception.ApiException;

public class ProductNotFoundException extends ApiException {

    public ProductNotFoundException(String sku) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "Produto com SKU '%s' não encontrado.".formatted(sku), sku);
    }
}
