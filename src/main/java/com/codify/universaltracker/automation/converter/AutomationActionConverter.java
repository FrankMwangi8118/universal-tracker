package com.codify.universaltracker.automation.converter;

import com.codify.universaltracker.automation.enums.AutomationAction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AutomationActionConverter implements AttributeConverter<AutomationAction, String> {

    @Override
    public String convertToDatabaseColumn(AutomationAction attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public AutomationAction convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AutomationAction.valueOf(dbData.toUpperCase());
    }
}
