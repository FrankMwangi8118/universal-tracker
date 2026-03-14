package com.codify.universaltracker.entry.dto;

import com.codify.universaltracker.entry.enums.EntryStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record UpdateEntryRequest(
        Instant entryDate,
        EntryStatus status,
        List<String> tags,
        String notes,
        Map<String, Object> metadata,
        Map<String, Object> fields   // only provided fields are updated
) {}
