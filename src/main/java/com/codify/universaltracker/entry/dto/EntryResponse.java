package com.codify.universaltracker.entry.dto;

import com.codify.universaltracker.entry.entity.Entry;
import com.codify.universaltracker.entry.enums.EntryStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EntryResponse(
        UUID id,
        UUID trackerId,
        UUID userId,
        Instant entryDate,
        EntryStatus status,
        List<String> tags,
        String notes,
        Map<String, Object> metadata,
        Map<String, FieldValueDisplay> fields,
        Instant createdAt,
        Instant updatedAt
) {
    public static EntryResponse from(Entry entry, Map<String, FieldValueDisplay> fields) {
        return new EntryResponse(
                entry.getId(),
                entry.getTrackerId(),
                entry.getUserId(),
                entry.getEntryDate(),
                entry.getStatus(),
                entry.getTags(),
                entry.getNotes(),
                entry.getMetadata(),
                fields,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
