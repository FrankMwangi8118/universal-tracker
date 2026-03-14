package com.codify.universaltracker.validation.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateValidationRuleRequest(
        Map<String, Object> ruleParams,

        @Size(max = 500)
        String errorMessage,

        Integer priority,
        Boolean isActive
) {}
