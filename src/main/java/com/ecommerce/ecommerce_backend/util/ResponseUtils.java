package com.ecommerce.ecommerce_backend.util;

import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseUtils {

    private ResponseUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static <T>ResponseEntity<ApiResponse<T>> success (T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    public static <T>ResponseEntity<ApiResponse<T>> created (T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, message));
    }

    public static <T> ResponseEntity<ApiResponse<T>> error (String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, status.value()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message, HttpStatus.NOT_FOUND.value()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, HttpStatus.BAD_REQUEST.value()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> unauthorized (String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message, HttpStatus.UNAUTHORIZED.value()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(message, HttpStatus.FORBIDDEN.value()));
    }
}
