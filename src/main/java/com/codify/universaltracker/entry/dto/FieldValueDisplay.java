package com.codify.universaltracker.entry.dto;

import com.codify.universaltracker.field.enums.FieldType;

public record FieldValueDisplay(
        Object value,
        String display,
        String fieldType,
        String color      // populated for dropdown/color fields
) {
    public static FieldValueDisplay of(Object value, String display, FieldType type) {
        return new FieldValueDisplay(value, display, type.name().toLowerCase(), null);
    }

    public static FieldValueDisplay of(Object value, String display, FieldType type, String color) {
        return new FieldValueDisplay(value, display, type.name().toLowerCase(), color);
    }
}
