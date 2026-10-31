package com.ecommerce.ecommerce_backend.dto.response;

import java.time.LocalDateTime;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String path;
    private Integer statusCode;

    // Constructors
    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, T data, LocalDateTime timestamp, String path, Integer statusCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
        this.path = path;
        this.statusCode = statusCode;
    }

    // ========== Success methods ==========
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.message = message;
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation successful");
    }

    public static <T> ApiResponse<T> success(String message) {
        return success(null, message);
    }

    // ========== Error methods ==========
    public static <T> ApiResponse<T> error(String message, Integer statusCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.message = message;
        response.statusCode = statusCode;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(message, 400);
    }

    // ========== Getters & Setters ==========
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
}