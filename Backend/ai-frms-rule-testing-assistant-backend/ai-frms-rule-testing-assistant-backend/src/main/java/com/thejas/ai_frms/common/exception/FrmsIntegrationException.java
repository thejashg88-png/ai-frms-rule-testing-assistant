package com.thejas.ai_frms.common.exception;

public class FrmsIntegrationException extends RuntimeException {

    public FrmsIntegrationException(String message) {
        super(message);
    }

    public FrmsIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}