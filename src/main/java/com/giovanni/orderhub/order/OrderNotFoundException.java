package com.giovanni.orderhub.order;

import com.giovanni.orderhub.shared.error.NotFoundException;

public class OrderNotFoundException extends NotFoundException {

    public OrderNotFoundException(Long id) {
        super("Pedido %d não encontrado.".formatted(id));
    }
}
