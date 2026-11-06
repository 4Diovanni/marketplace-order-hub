package com.giovanni.orderhub.exception.order;

import com.giovanni.orderhub.enums.ErrorCode;
import com.giovanni.orderhub.exception.ApiException;
import java.util.Collection;

public class UnknownSkuException extends ApiException {

    public UnknownSkuException(Collection<String> skus) {
        super(ErrorCode.UNKNOWN_SKU,
                "O pedido tem SKUs que não existem no cadastro: %s.".formatted(String.join(", ", skus)),
                String.join(", ", skus));
    }
}
