package com.codify.universaltracker.entry.converter;

import com.codify.universaltracker.entry.enums.EntryStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EntryStatusConverter implements AttributeConverter<EntryStatus, String> {

    @Override
    public String convertToDatabaseColumn(EntryStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public EntryStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EntryStatus.valueOf(dbData.toUpperCase());
    }
}
