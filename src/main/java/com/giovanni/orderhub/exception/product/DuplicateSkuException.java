package com.giovanni.orderhub.exception.product;

import com.giovanni.orderhub.enums.ErrorCode;
import com.giovanni.orderhub.exception.ApiException;

public class DuplicateSkuException extends ApiException {

    public DuplicateSkuException(String sku) {
        super(ErrorCode.DUPLICATE_SKU, "Já existe um produto com SKU '%s'.".formatted(sku), sku);
    }
}
