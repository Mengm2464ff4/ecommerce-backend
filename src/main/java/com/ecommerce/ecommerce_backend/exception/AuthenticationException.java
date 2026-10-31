package com.ecommerce.ecommerce_backend.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends CustomException {

    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR");
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_ERROR");
        this.initCause(cause);
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid username or password");
    }

    public static AuthenticationException tokenExpired() {
        return new AuthenticationException("JWT token has expired");
    }

    public static AuthenticationException invalidToken() {
        return new AuthenticationException("Invalid JWT token");
    }

    public static AuthenticationException userDisabled() {
        return new AuthenticationException("User account is disabled");
    }
}