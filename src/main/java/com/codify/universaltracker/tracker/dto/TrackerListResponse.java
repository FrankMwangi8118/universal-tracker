package com.codify.universaltracker.tracker.dto;

import com.codify.universaltracker.tracker.entity.Tracker;

import java.util.UUID;

public record TrackerListResponse(
        UUID id,
        UUID domainId,
        String name,
        String slug,
        String icon,
        String description,
        String entryNameSingular,
        String entryNamePlural,
        Boolean isActive,
        Integer sortOrder,
        long fieldCount,
        long entryCount
) {
    public static TrackerListResponse from(Tracker tracker, long fieldCount, long entryCount) {
        return new TrackerListResponse(
                tracker.getId(),
                tracker.getDomainId(),
                tracker.getName(),
                tracker.getSlug(),
                tracker.getIcon(),
                tracker.getDescription(),
                tracker.getEntryNameSingular(),
                tracker.getEntryNamePlural(),
                tracker.getIsActive(),
                tracker.getSortOrder(),
                fieldCount,
                entryCount
        );
    }
}
