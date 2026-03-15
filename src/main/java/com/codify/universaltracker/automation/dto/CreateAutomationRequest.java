package com.codify.universaltracker.automation.dto;

import com.codify.universaltracker.automation.enums.AutomationAction;
import com.codify.universaltracker.automation.enums.AutomationTrigger;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CreateAutomationRequest(

        @NotBlank @Size(max = 200)
        String name,

        String description,

        @NotNull
        AutomationTrigger triggerEvent,

        Map<String, Object> triggerConfig,

        // Each condition: {"field": "slug", "operator": "eq|gt|lt|contains", "value": ...}
        List<Map<String, Object>> conditions,

        @NotNull
        AutomationAction actionType,

        @NotNull
        Map<String, Object> actionParams
) {}
