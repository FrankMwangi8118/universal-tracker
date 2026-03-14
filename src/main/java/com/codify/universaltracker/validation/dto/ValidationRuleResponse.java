package com.codify.universaltracker.validation.dto;

import com.codify.universaltracker.validation.entity.ValidationRule;
import com.codify.universaltracker.validation.enums.ValidationRuleType;

import java.util.Map;
import java.util.UUID;

public record ValidationRuleResponse(
        UUID id,
        UUID fieldDefinitionId,
        ValidationRuleType ruleType,
        Map<String, Object> ruleParams,
        String errorMessage,
        Integer priority,
        Boolean isActive
) {
    public static ValidationRuleResponse from(ValidationRule rule) {
        return new ValidationRuleResponse(
                rule.getId(),
                rule.getFieldDefinitionId(),
                rule.getRuleType(),
                rule.getRuleParams(),
                rule.getErrorMessage(),
                rule.getPriority(),
                rule.getIsActive()
        );
    }
}
