package com.codify.universaltracker.automation.converter;

import com.codify.universaltracker.automation.enums.AutomationTrigger;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AutomationTriggerConverter implements AttributeConverter<AutomationTrigger, String> {

    @Override
    public String convertToDatabaseColumn(AutomationTrigger attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public AutomationTrigger convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AutomationTrigger.valueOf(dbData.toUpperCase());
    }
}
