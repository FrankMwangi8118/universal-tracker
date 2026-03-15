package com.codify.universaltracker.entry.dto;

import com.codify.universaltracker.entry.entity.Entry;
import com.codify.universaltracker.entry.enums.EntryStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EntryListResponse(
        UUID id,
        UUID trackerId,
        Instant entryDate,
        EntryStatus status,
        List<String> tags,
        String notes,
        Map<String, FieldValueDisplay> fields,  // only isPrimaryDisplay fields
        Instant createdAt
) {
    public static EntryListResponse from(Entry entry, Map<String, FieldValueDisplay> primaryFields) {
        return new EntryListResponse(
                entry.getId(),
                entry.getTrackerId(),
                entry.getEntryDate(),
                entry.getStatus(),
                entry.getTags(),
                entry.getNotes(),
                primaryFields,
                entry.getCreatedAt()
        );
    }
}
