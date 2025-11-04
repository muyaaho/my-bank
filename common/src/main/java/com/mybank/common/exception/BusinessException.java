package com.mybank.common.exception;

import lombok.Getter;

/**
 * Base business exception for all microservices
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final String detail;

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
        this.detail = null;
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.detail = null;
    }

    public BusinessException(String errorCode, String message, String detail) {
        super(message);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.detail = null;
    }
}
