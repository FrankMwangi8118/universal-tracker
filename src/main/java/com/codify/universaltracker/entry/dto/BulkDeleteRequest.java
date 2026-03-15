package com.codify.universaltracker.entry.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record BulkDeleteRequest(
        @NotEmpty(message = "Entry IDs list must not be empty")
        List<UUID> ids
) {}
