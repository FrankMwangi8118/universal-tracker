package com.codify.universaltracker.automation.dto;

import com.codify.universaltracker.automation.enums.AutomationAction;
import com.codify.universaltracker.automation.enums.AutomationTrigger;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record UpdateAutomationRequest(

        @Size(max = 200)
        String name,

        String description,

        AutomationTrigger triggerEvent,

        Map<String, Object> triggerConfig,

        List<Map<String, Object>> conditions,

        AutomationAction actionType,

        Map<String, Object> actionParams,

        Boolean isActive
) {}
