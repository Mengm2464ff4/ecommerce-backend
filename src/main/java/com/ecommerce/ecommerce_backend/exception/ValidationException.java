package com.ecommerce.ecommerce_backend.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ValidationException extends CustomException{

    private final Map<String, String> validationErrors;

    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        this.validationErrors = null;
    }

    public ValidationException(String message, Map<String, String> validationErrors) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", validationErrors);
        this.validationErrors = validationErrors;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }
}
