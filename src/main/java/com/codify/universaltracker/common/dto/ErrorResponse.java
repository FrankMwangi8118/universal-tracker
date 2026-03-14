package com.codify.universaltracker.common.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String error,
        String code,
        Map<String, Object> details,
        Instant timestamp
) {
    public static ErrorResponse of(String error, String code) {
        return new ErrorResponse(error, code, null, Instant.now());
    }

    public static ErrorResponse of(String error, String code, Map<String, Object> details) {
        return new ErrorResponse(error, code, details, Instant.now());
    }
}
