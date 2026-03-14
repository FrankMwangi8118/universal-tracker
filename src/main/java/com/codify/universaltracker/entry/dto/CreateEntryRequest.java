package com.codify.universaltracker.entry.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CreateEntryRequest(
        Instant entryDate,
        List<String> tags,
        String notes,
        Map<String, Object> metadata,
        Map<String, Object> fields   // key = field slug, value = raw submitted value
) {}
