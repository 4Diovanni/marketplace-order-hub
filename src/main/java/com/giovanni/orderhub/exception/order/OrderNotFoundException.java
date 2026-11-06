package com.giovanni.orderhub.exception.order;

import com.giovanni.orderhub.enums.ErrorCode;
import com.giovanni.orderhub.exception.ApiException;

public class OrderNotFoundException extends ApiException {

    public OrderNotFoundException(Long id) {
        super(ErrorCode.ORDER_NOT_FOUND, "Pedido %d não encontrado.".formatted(id), id);
    }
}
