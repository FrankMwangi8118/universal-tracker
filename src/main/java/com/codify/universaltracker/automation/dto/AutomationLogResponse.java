package com.codify.universaltracker.automation.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AutomationLogResponse(
        UUID id,
        UUID automationId,
        UUID entryId,
        Map<String, Object> triggerData,
        Map<String, Object> actionResult,
        String status,
        String errorMessage,
        Instant executedAt
) {}
