package com.giovanni.orderhub.exception;

import com.giovanni.orderhub.enums.ErrorCode;

/**
 * Base de todas as exceções de negócio.
 *
 * <p>Carrega duas mensagens: a técnica ({@link #getMessage()}, vai no {@code detail} e no log)
 * e a mensagem para o seller ({@link #getSellerMessage()}, pronta para mostrar no front).
 */
public abstract class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String sellerMessage;

    protected ApiException(ErrorCode errorCode, String technicalDetail, Object... sellerMessageArgs) {
        super(technicalDetail);
        this.errorCode = errorCode;
        this.sellerMessage = errorCode.formatMessage(sellerMessageArgs);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getSellerMessage() {
        return sellerMessage;
    }
}
