package com.codify.universaltracker.automation.dto;

import com.codify.universaltracker.automation.enums.AutomationAction;
import com.codify.universaltracker.automation.enums.AutomationTrigger;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AutomationResponse(
        UUID id,
        UUID trackerId,
        String name,
        String description,
        AutomationTrigger triggerEvent,
        Map<String, Object> triggerConfig,
        List<Map<String, Object>> conditions,
        AutomationAction actionType,
        Map<String, Object> actionParams,
        Boolean isActive,
        Instant lastTriggered,
        Integer runCount,
        Instant createdAt,
        Instant updatedAt
) {}
