package com.codify.universaltracker.view.entity;

import com.codify.universaltracker.view.enums.ViewType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ViewTypeConverter implements AttributeConverter<ViewType, String> {

    @Override
    public String convertToDatabaseColumn(ViewType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public ViewType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ViewType.valueOf(dbData.toUpperCase());
    }
}
