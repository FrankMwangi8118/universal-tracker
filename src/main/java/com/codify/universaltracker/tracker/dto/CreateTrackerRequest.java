package com.codify.universaltracker.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTrackerRequest(

        @NotNull(message = "domainId is required")
        UUID domainId,

        @NotBlank(message = "Name is required")
        @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
        String name,

        @Size(max = 50)
        String icon,

        String description,

        @Size(max = 50)
        String entryNameSingular,

        @Size(max = 50)
        String entryNamePlural
) {}
