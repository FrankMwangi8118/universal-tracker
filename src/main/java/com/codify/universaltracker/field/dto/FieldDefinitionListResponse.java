package com.codify.universaltracker.field.dto;

import com.codify.universaltracker.field.entity.FieldDefinition;
import com.codify.universaltracker.field.enums.FieldType;

import java.util.List;
import java.util.UUID;

public record FieldDefinitionListResponse(
        UUID id,
        UUID trackerId,
        String name,
        String slug,
        FieldType fieldType,
        Boolean isRequired,
        Boolean isPrimaryDisplay,
        Boolean isSummable,
        Integer sortOrder,
        Boolean isActive,
        List<FieldOptionResponse> options
) {
    public static FieldDefinitionListResponse from(FieldDefinition field, List<FieldOptionResponse> options) {
        return new FieldDefinitionListResponse(
                field.getId(),
                field.getTrackerId(),
                field.getName(),
                field.getSlug(),
                field.getFieldType(),
                field.getIsRequired(),
                field.getIsPrimaryDisplay(),
                field.getIsSummable(),
                field.getSortOrder(),
                field.getIsActive(),
                options
        );
    }
}
