package com.codify.universaltracker.view.dto;

import com.codify.universaltracker.view.enums.ViewType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CreateViewRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @NotNull(message = "viewType is required")
        ViewType viewType,

        List<Map<String, Object>> columns,
        List<Map<String, Object>> sortRules,
        List<Map<String, Object>> filterRules,
        Map<String, Object> groupBy,
        List<Map<String, Object>> aggregations,
        Map<String, Object> chartConfig,
        Boolean isDefault,
        Integer sortOrder
) {}
