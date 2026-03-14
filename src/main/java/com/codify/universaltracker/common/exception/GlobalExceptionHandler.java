package com.codify.universaltracker.common.exception;

import com.codify.universaltracker.common.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404 — resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    // 409 — duplicate resource
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        log.debug("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getMessage(), "DUPLICATE_RESOURCE"));
    }

    // 400 — field-level validation failures (custom)
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.debug("Validation error: {}", ex.getMessage());
        Map<String, Object> details = new HashMap<>();
        ex.getFieldErrors().forEach((field, errors) -> details.put(field, errors));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Validation failed", "VALIDATION_ERROR", details));
    }

    // 422 — business rule violation
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        log.debug("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(ex.getMessage(), "BUSINESS_RULE_VIOLATION"));
    }

    // 400 — Jakarta Bean Validation on @RequestBody (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.merge(error.getField(), List.of(error.getDefaultMessage()),
                    (existing, newVal) -> {
                        List<String> combined = new java.util.ArrayList<>((List<String>) existing);
                        combined.addAll((List<String>) newVal);
                        return combined;
                    });
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Request validation failed", "VALIDATION_ERROR", details));
    }

    // 400 — malformed JSON body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Malformed or missing request body", "INVALID_REQUEST_BODY"));
    }

    // 400 — path variable / query param type mismatch (e.g., non-UUID where UUID expected)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' has invalid value: %s", ex.getName(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(message, "INVALID_PARAMETER"));
    }

    // 400 — missing required header (e.g., X-User-Id)
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Missing required header: " + ex.getHeaderName(), "MISSING_HEADER"));
    }

    // 404 — no matching route
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoRoute(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("Endpoint not found: " + ex.getResourcePath(), "ENDPOINT_NOT_FOUND"));
    }

    // 405 — wrong HTTP method
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of("Method not allowed: " + ex.getMethod(), "METHOD_NOT_ALLOWED"));
    }

    // Catch-all — 422 instead of 500; log the full stack trace for investigation
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error — investigate immediately", ex);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of("An unexpected error occurred. Please try again.", "UNEXPECTED_ERROR"));
    }
}
