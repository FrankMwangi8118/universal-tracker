package com.codify.universaltracker.field.dto;

import com.codify.universaltracker.field.enums.FieldType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CreateFieldRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 1, max = 100)
        String name,

        @NotNull(message = "fieldType is required")
        FieldType fieldType,

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

        @Valid
        List<CreateFieldOptionRequest> options
) {}
