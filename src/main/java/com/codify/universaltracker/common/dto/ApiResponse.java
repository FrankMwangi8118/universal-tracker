package com.codify.universaltracker.common.dto;

import java.util.Map;

public record ApiResponse<T>(
        boolean success,
        T data,
        Map<String, Object> meta
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
        return new ApiResponse<>(true, data, meta);
    }

    public static <T> ApiResponse<T> paginated(T data, long totalElements, int page, int size) {
        long totalPages = size == 0 ? 0 : (long) Math.ceil((double) totalElements / size);
        Map<String, Object> meta = Map.of(
                "page", page,
                "size", size,
                "total_elements", totalElements,
                "total_pages", totalPages
        );
        return new ApiResponse<>(true, data, meta);
    }
}
