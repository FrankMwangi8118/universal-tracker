package com.codify.universaltracker.tracker.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateTrackerRequest(

        @Size(min = 1, max = 100)
        String name,

        @Size(max = 50)
        String icon,

        String description,

        @Size(max = 50)
        String entryNameSingular,

        @Size(max = 50)
        String entryNamePlural,

        Map<String, Object> summaryConfig,

        Integer sortOrder,

        Boolean isActive,

        String defaultDateField
) {}
