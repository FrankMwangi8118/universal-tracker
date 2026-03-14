package com.codify.universaltracker.field.entity;

import com.codify.universaltracker.field.enums.FieldType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FieldTypeConverter implements AttributeConverter<FieldType, String> {

    @Override
    public String convertToDatabaseColumn(FieldType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public FieldType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : FieldType.valueOf(dbData.toUpperCase());
    }
}
