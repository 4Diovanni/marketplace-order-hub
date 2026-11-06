package com.giovanni.orderhub.exception;

import com.giovanni.orderhub.enums.ErrorCode;

/**
 * Base class of all business exceptions.
 *
 * <p>Carries two messages: the technical one ({@link #getMessage()}, goes to {@code detail} and the log)
 * and the seller message ({@link #getSellerMessage()}, ready to show in the front end).
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
