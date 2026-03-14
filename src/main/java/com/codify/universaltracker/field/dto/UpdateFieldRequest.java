package com.codify.universaltracker.field.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record UpdateFieldRequest(

        @Size(min = 1, max = 100)
        String name,

        Boolean isRequired,
        Boolean isUnique,
        Boolean isFilterable,
        Boolean isSummable,
        Boolean isPrimaryDisplay,

        Object defaultValue,

        @Size(max = 100)
        String displayFormat,

        @Size(max = 200)
        String placeholder,

        String helpText,

        @Size(max = 3)
        String currencyCode,

        BigDecimal minValue,
        BigDecimal maxValue,

        Map<String, Object> conditionalLogic,
        String formulaExpression,

        Integer sortOrder,
        Boolean isActive,

        @Valid
        List<CreateFieldOptionRequest> options
) {}
