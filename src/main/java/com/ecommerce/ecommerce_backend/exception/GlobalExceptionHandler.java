package com.ecommerce.ecommerce_backend.exception;

import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle Resource Not Found Exception
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Handle Custom Exception
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException ex) {
        log.error("Custom exception: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), ex.getStatus().value());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    // Handle Validation Errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        log.error("Validation error: {}", ex.getMessage());

        Map<String, List<String>> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);
        }

        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            String objectName = error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.computeIfAbsent(objectName, k -> new ArrayList<>()).add(errorMessage);
        }

        ApiResponse<Map<String, List<String>>> response = ApiResponse.error(
                "Validation failed", HttpStatus.BAD_REQUEST.value());
        response.setData(errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle Constraint Violation Exception
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex) {

        log.error("Constraint violation: {}", ex.getMessage());

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (error1, error2) -> error1 + "; " + error2
                ));

        ApiResponse<Map<String, String>> response = ApiResponse.error(
                "Validation failed", HttpStatus.BAD_REQUEST.value());
        response.setData(errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle Bad Credentials
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.error("Bad credentials: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(
                "Invalid username or password", HttpStatus.UNAUTHORIZED.value());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // Handle Username Not Found
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        log.error("Username not found: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(
                "User not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Handle Access Denied
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.error(
                "Access denied. You don't have permission to access this resource.",
                HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // Handle Data Integrity Violation
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {

        log.error("Data integrity violation: {}", ex.getMessage());

        String message = "Database operation failed due to data integrity violation.";
        if (ex.getCause() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage.contains("duplicate key")) {
                message = "Duplicate entry found. The value already exists.";
            } else if (causeMessage.contains("foreign key")) {
                message = "This operation violates a foreign key constraint.";
            } else if (causeMessage.contains("not null")) {
                message = "Required field cannot be null.";
            }
        }

        ApiResponse<Void> response = ApiResponse.error(message, HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // Handle Method Argument Type Mismatch
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {

        log.error("Argument type mismatch: {}", ex.getMessage());

        String message = String.format(
                "Parameter '%s' must be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        ApiResponse<Void> response = ApiResponse.error(message, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle Missing Request Parameter
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {

        log.error("Missing parameter: {}", ex.getMessage());

        String message = String.format(
                "Required parameter '%s' of type '%s' is missing",
                ex.getParameterName(),
                ex.getParameterType()
        );

        ApiResponse<Void> response = ApiResponse.error(message, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle HTTP Message Not Readable
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {

        log.error("Message not readable: {}", ex.getMessage());

        String message = "Malformed JSON request. Please check your request body.";
        if (ex.getCause() != null) {
            message = ex.getCause().getMessage();
        }

        ApiResponse<Void> response = ApiResponse.error(message, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handle HTTP Request Method Not Supported
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {

        log.error("Method not supported: {}", ex.getMessage());

        String message = String.format(
                "HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(),
                String.join(", ", ex.getSupportedMethods() != null ? ex.getSupportedMethods() : new String[]{"GET", "POST", "PUT", "DELETE"})
        );

        ApiResponse<Void> response = ApiResponse.error(message, HttpStatus.METHOD_NOT_ALLOWED.value());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    // Handle No Handler Found (404)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.error("No handler found: {}", ex.getMessage());

        String message = String.format("Endpoint '%s' not found", ex.getRequestURL());

        ApiResponse<Void> response = ApiResponse.error(message, HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Handle Max Upload Size Exceeded
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {

        log.error("File too large: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                "File size exceeds the maximum allowed limit (10MB)",
                HttpStatus.PAYLOAD_TOO_LARGE.value());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    // Handle Generic Runtime Exceptions
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.error(
                "An unexpected error occurred: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Handle All Other Exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ApiResponse<Void> response = ApiResponse.error(
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}