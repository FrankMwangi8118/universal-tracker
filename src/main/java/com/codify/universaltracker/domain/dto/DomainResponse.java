package com.codify.universaltracker.domain.dto;

import com.codify.universaltracker.domain.entity.Domain;

import java.time.Instant;
import java.util.UUID;

public record DomainResponse(
        UUID id,
        UUID userId,
        String name,
        String slug,
        String icon,
        String color,
        String description,
        Integer sortOrder,
        Boolean isActive,
        long trackerCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static DomainResponse from(Domain domain, long trackerCount) {
        return new DomainResponse(
                domain.getId(),
                domain.getUserId(),
                domain.getName(),
                domain.getSlug(),
                domain.getIcon(),
                domain.getColor(),
                domain.getDescription(),
                domain.getSortOrder(),
                domain.getIsActive(),
                trackerCount,
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
