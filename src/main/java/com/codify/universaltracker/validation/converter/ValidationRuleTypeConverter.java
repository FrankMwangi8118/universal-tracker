package com.codify.universaltracker.validation.converter;

import com.codify.universaltracker.validation.enums.ValidationRuleType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ValidationRuleTypeConverter implements AttributeConverter<ValidationRuleType, String> {

    @Override
    public String convertToDatabaseColumn(ValidationRuleType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public ValidationRuleType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ValidationRuleType.valueOf(dbData.toUpperCase());
    }
}
