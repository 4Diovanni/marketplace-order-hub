package com.giovanni.orderhub.exception;

import java.util.Collection;

public class UnknownSkuException extends BusinessRuleException {

    public UnknownSkuException(Collection<String> skus) {
        super("O pedido tem SKUs que não existem no cadastro: %s.".formatted(String.join(", ", skus)));
    }
}
