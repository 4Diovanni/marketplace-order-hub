package com.giovanni.orderhub.order;

import com.giovanni.orderhub.shared.error.BusinessRuleException;
import java.util.Collection;

public class UnknownSkuException extends BusinessRuleException {

    public UnknownSkuException(Collection<String> skus) {
        super("O pedido tem SKUs que não existem no cadastro: %s.".formatted(String.join(", ", skus)));
    }
}
