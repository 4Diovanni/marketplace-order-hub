package com.giovanni.orderhub.exception.integration;

import com.giovanni.orderhub.dto.error.FieldViolation;
import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.enums.ErrorCode;
import com.giovanni.orderhub.exception.ApiException;
import java.util.List;

/** The JSON sent by the marketplace does not follow the channel contract. Becomes HTTP 400 with the field list. */
public class InvalidPayloadException extends ApiException {

    private final List<FieldViolation> violations;

    public InvalidPayloadException(Channel channel, String technicalDetail, List<FieldViolation> violations) {
        super(ErrorCode.INVALID_MARKETPLACE_PAYLOAD, technicalDetail, channel.getDisplayName());
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }
}
