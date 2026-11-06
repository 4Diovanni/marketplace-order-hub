package com.giovanni.orderhub.exception;

public class OrderNotFoundException extends NotFoundException {

    public OrderNotFoundException(Long id) {
        super("Pedido %d não encontrado.".formatted(id));
    }
}
