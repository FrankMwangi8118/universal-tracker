package com.codify.universaltracker.tracker.dto;

import com.codify.universaltracker.tracker.entity.Tracker;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TrackerResponse(
        UUID id,
        UUID domainId,
        UUID userId,
        String name,
        String slug,
        String icon,
        String description,
        String entryNameSingular,
        String entryNamePlural,
        String defaultDateField,
        Map<String, Object> summaryConfig,
        Integer sortOrder,
        Boolean isActive,
        long fieldCount,
        long entryCount,
        Instant lastEntryDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static TrackerResponse from(Tracker tracker, long fieldCount, long entryCount, Instant lastEntryDate) {
        return new TrackerResponse(
                tracker.getId(),
                tracker.getDomainId(),
                tracker.getUserId(),
                tracker.getName(),
                tracker.getSlug(),
                tracker.getIcon(),
                tracker.getDescription(),
                tracker.getEntryNameSingular(),
                tracker.getEntryNamePlural(),
                tracker.getDefaultDateField(),
                tracker.getSummaryConfig(),
                tracker.getSortOrder(),
                tracker.getIsActive(),
                fieldCount,
                entryCount,
                lastEntryDate,
                tracker.getCreatedAt(),
                tracker.getUpdatedAt()
        );
    }
}
