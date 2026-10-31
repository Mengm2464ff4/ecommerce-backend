package com.ecommerce.ecommerce_backend.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Object data;

    // 1. Only message
    public CustomException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = "BAD_REQUEST";
        this.data = null;
    }

    // 2. message + status
    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = status.name();
        this.data = null;
    }

    // 3. message + status + errorCode
    public CustomException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.data = null;
    }

    // 4. Full constructor
    public CustomException(
            String message,
            HttpStatus status,
            String errorCode,
            Object data
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.data = data;
    }

    // Getters
    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }

    // Static factory methods
    public static CustomException badRequest(String message) {
        return new CustomException(
                message,
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST"
        );
    }

    public static CustomException unauthorized(String message) {
        return new CustomException(
                message,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED"
        );
    }

    public static CustomException forbidden(String message) {
        return new CustomException(
                message,
                HttpStatus.FORBIDDEN,
                "FORBIDDEN"
        );
    }

    public static CustomException notFound(String message) {
        return new CustomException(
                message,
                HttpStatus.NOT_FOUND,
                "NOT_FOUND"
        );
    }

    public static CustomException conflict(String message) {
        return new CustomException(
                message,
                HttpStatus.CONFLICT,
                "CONFLICT"
        );
    }

    public static CustomException internalServerError(String message) {
        return new CustomException(
                message,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR"
        );
    }
}