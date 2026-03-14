package com.codify.universaltracker.validation.dto;

import com.codify.universaltracker.validation.enums.ValidationRuleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateValidationRuleRequest(

        @NotNull(message = "ruleType is required")
        ValidationRuleType ruleType,

        Map<String, Object> ruleParams,

        @Size(max = 500)
        String errorMessage,

        Integer priority
) {}
