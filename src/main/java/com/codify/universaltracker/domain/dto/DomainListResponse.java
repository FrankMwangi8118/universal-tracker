package com.codify.universaltracker.domain.dto;

import com.codify.universaltracker.domain.entity.Domain;

import java.util.UUID;

public record DomainListResponse(
        UUID id,
        String name,
        String slug,
        String icon,
        String color,
        Boolean isActive,
        Integer sortOrder,
        long trackerCount
) {
    public static DomainListResponse from(Domain domain, long trackerCount) {
        return new DomainListResponse(
                domain.getId(),
                domain.getName(),
                domain.getSlug(),
                domain.getIcon(),
                domain.getColor(),
                domain.getIsActive(),
                domain.getSortOrder(),
                trackerCount
        );
    }
}
