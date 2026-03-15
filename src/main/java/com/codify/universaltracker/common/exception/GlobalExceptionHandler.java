package com.codify.universaltracker.common.exception;

import com.codify.universaltracker.common.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return json(HttpStatus.NOT_FOUND, ErrorResponse.of(ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        log.debug("Duplicate resource: {}", ex.getMessage());
        return json(HttpStatus.CONFLICT, ErrorResponse.of(ex.getMessage(), "DUPLICATE_RESOURCE"));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.debug("Validation error: {}", ex.getMessage());
        Map<String, Object> details = new HashMap<>();
        ex.getFieldErrors().forEach((field, errors) -> details.put(field, errors));
        return json(HttpStatus.BAD_REQUEST, ErrorResponse.of("Validation failed", "VALIDATION_ERROR", details));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        log.debug("Business rule violation: {}", ex.getMessage());
        return json(HttpStatus.UNPROCESSABLE_ENTITY, ErrorResponse.of(ex.getMessage(), "BUSINESS_RULE_VIOLATION"));
    }

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
        return json(HttpStatus.BAD_REQUEST, ErrorResponse.of("Request validation failed", "VALIDATION_ERROR", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        return json(HttpStatus.BAD_REQUEST, ErrorResponse.of("Malformed or missing request body", "INVALID_REQUEST_BODY"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' has invalid value: %s", ex.getName(), ex.getValue());
        return json(HttpStatus.BAD_REQUEST, ErrorResponse.of(message, "INVALID_PARAMETER"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return json(HttpStatus.BAD_REQUEST, ErrorResponse.of("Missing required header: " + ex.getHeaderName(), "MISSING_HEADER"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoRoute(NoResourceFoundException ex) {
        return json(HttpStatus.NOT_FOUND, ErrorResponse.of("Endpoint not found: " + ex.getResourcePath(), "ENDPOINT_NOT_FOUND"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return json(HttpStatus.METHOD_NOT_ALLOWED, ErrorResponse.of("Method not allowed: " + ex.getMethod(), "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error — investigate immediately", ex);
        return json(HttpStatus.UNPROCESSABLE_ENTITY, ErrorResponse.of("An unexpected error occurred. Please try again.", "UNEXPECTED_ERROR"));
    }

    private static ResponseEntity<ErrorResponse> json(HttpStatus status, ErrorResponse body) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
