package com.codify.universaltracker.field.dto;

import com.codify.universaltracker.field.entity.FieldOption;

import java.util.UUID;

public record FieldOptionResponse(
        UUID id,
        String label,
        String value,
        String color,
        String icon,
        Integer sortOrder,
        Boolean isDefault,
        Boolean isActive
) {
    public static FieldOptionResponse from(FieldOption option) {
        return new FieldOptionResponse(
                option.getId(),
                option.getLabel(),
                option.getValue(),
                option.getColor(),
                option.getIcon(),
                option.getSortOrder(),
                option.getIsDefault(),
                option.getIsActive()
        );
    }
}
