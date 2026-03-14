package com.codify.universaltracker.common.exception;

import java.util.List;
import java.util.Map;

public class ValidationException extends RuntimeException {

    private final Map<String, List<String>> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }

    public ValidationException(String field, String error) {
        super("Validation failed");
        this.fieldErrors = Map.of(field, List.of(error));
    }

    public ValidationException(Map<String, List<String>> fieldErrors) {
        super("Validation failed");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
}
