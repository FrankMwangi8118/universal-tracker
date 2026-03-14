package com.codify.universaltracker.field.dto;

import com.codify.universaltracker.field.entity.FieldDefinition;
import com.codify.universaltracker.field.enums.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FieldDefinitionResponse(
        UUID id,
        UUID trackerId,
        String name,
        String slug,
        FieldType fieldType,
        Boolean isRequired,
        Boolean isUnique,
        Boolean isFilterable,
        Boolean isSummable,
        Boolean isPrimaryDisplay,
        Object defaultValue,
        String displayFormat,
        String placeholder,
        String helpText,
        String currencyCode,
        BigDecimal minValue,
        BigDecimal maxValue,
        Integer sortOrder,
        Boolean isActive,
        Map<String, Object> conditionalLogic,
        String formulaExpression,
        List<FieldOptionResponse> options,
        Instant createdAt,
        Instant updatedAt
) {
    public static FieldDefinitionResponse from(FieldDefinition field, List<FieldOptionResponse> options) {
        return new FieldDefinitionResponse(
                field.getId(),
                field.getTrackerId(),
                field.getName(),
                field.getSlug(),
                field.getFieldType(),
                field.getIsRequired(),
                field.getIsUnique(),
                field.getIsFilterable(),
                field.getIsSummable(),
                field.getIsPrimaryDisplay(),
                field.getDefaultValue(),
                field.getDisplayFormat(),
                field.getPlaceholder(),
                field.getHelpText(),
                field.getCurrencyCode(),
                field.getMinValue(),
                field.getMaxValue(),
                field.getSortOrder(),
                field.getIsActive(),
                field.getConditionalLogic(),
                field.getFormulaExpression(),
                options,
                field.getCreatedAt(),
                field.getUpdatedAt()
        );
    }
}
