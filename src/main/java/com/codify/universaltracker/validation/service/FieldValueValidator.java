package com.codify.universaltracker.validation.service;

import com.codify.universaltracker.field.entity.FieldDefinition;
import com.codify.universaltracker.field.enums.FieldType;
import com.codify.universaltracker.validation.entity.ValidationRule;
import com.codify.universaltracker.validation.repository.ValidationRuleRepository;
import com.codify.universaltracker.entry.repository.FieldValueRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FieldValueValidator {

    private final ValidationRuleRepository ruleRepository;
    private final FieldValueRepository fieldValueRepository;

    public FieldValueValidator(ValidationRuleRepository ruleRepository,
                               FieldValueRepository fieldValueRepository) {
        this.ruleRepository = ruleRepository;
        this.fieldValueRepository = fieldValueRepository;
    }

    /**
     * Runs all active validation rules for a field against the submitted value.
     * Returns a list of error messages — empty list means valid.
     */
    public List<String> validate(FieldDefinition field, Object value, UUID entryId) {
        List<String> errors = new ArrayList<>();

        // Type-level format validation first
        errors.addAll(validateTypeFormat(field.getFieldType(), value));

        // Run stored validation rules (ordered by priority)
        List<ValidationRule> rules = ruleRepository
                .findByFieldDefinitionIdAndIsActiveTrueOrderByPriorityAsc(field.getId());

        for (ValidationRule rule : rules) {
            String error = switch (rule.getRuleType()) {
                case REQUIRED -> validateRequired(value, rule);
                case MIN -> validateMin(value, rule);
                case MAX -> validateMax(value, rule);
                case MIN_LENGTH -> validateMinLength(value, rule);
                case MAX_LENGTH -> validateMaxLength(value, rule);
                case REGEX -> validateRegex(value, rule);
                case UNIQUE -> validateUnique(field.getId(), value, entryId, rule);
                case CUSTOM -> null; // Custom rules: log and skip for now
            };
            if (error != null) errors.add(error);
        }

        return errors;
    }

    // -------------------------------------------------------------------------
    // Type-level format validators

    private List<String> validateTypeFormat(FieldType type, Object value) {
        if (value == null) return List.of();
        String str = value.toString();

        return switch (type) {
            case EMAIL -> isValidEmail(str)
                    ? List.of()
                    : List.of("Invalid email address format");
            case URL -> isValidUrl(str)
                    ? List.of()
                    : List.of("Invalid URL format");
            case PHONE -> isValidPhone(str)
                    ? List.of()
                    : List.of("Invalid phone number format");
            case COLOR -> isValidHexColor(str)
                    ? List.of()
                    : List.of("Invalid color — must be a hex code (e.g. #1B5E20)");
            default -> List.of();
        };
    }

    private boolean isValidEmail(String value) {
        return value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidUrl(String value) {
        return value.matches("^https?://[^\\s/$.?#].[^\\s]*$");
    }

    private boolean isValidPhone(String value) {
        // Accepts E.164 format and common local formats
        return value.matches("^[+]?[0-9\\s\\-().]{7,20}$");
    }

    private boolean isValidHexColor(String value) {
        return value.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }

    // -------------------------------------------------------------------------
    // Rule-type validators

    private String validateRequired(Object value, ValidationRule rule) {
        if (value == null || value.toString().isBlank()) {
            return message(rule, "This field is required");
        }
        return null;
    }

    private String validateMin(Object value, ValidationRule rule) {
        if (value == null) return null;
        try {
            BigDecimal num = new BigDecimal(value.toString());
            BigDecimal min = getParam(rule.getRuleParams(), "value");
            if (min != null && num.compareTo(min) < 0) {
                return message(rule, "Value must be at least " + min);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private String validateMax(Object value, ValidationRule rule) {
        if (value == null) return null;
        try {
            BigDecimal num = new BigDecimal(value.toString());
            BigDecimal max = getParam(rule.getRuleParams(), "value");
            if (max != null && num.compareTo(max) > 0) {
                return message(rule, "Value must be at most " + max);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private String validateMinLength(Object value, ValidationRule rule) {
        if (value == null) return null;
        int len = value.toString().length();
        BigDecimal minLen = getParam(rule.getRuleParams(), "value");
        if (minLen != null && len < minLen.intValue()) {
            return message(rule, "Must be at least " + minLen.intValue() + " characters");
        }
        return null;
    }

    private String validateMaxLength(Object value, ValidationRule rule) {
        if (value == null) return null;
        int len = value.toString().length();
        BigDecimal maxLen = getParam(rule.getRuleParams(), "value");
        if (maxLen != null && len > maxLen.intValue()) {
            return message(rule, "Must be at most " + maxLen.intValue() + " characters");
        }
        return null;
    }

    private String validateRegex(Object value, ValidationRule rule) {
        if (value == null) return null;
        Object patternParam = rule.getRuleParams().get("pattern");
        if (patternParam == null) return null;
        try {
            if (!Pattern.matches(patternParam.toString(), value.toString())) {
                return message(rule, "Value does not match required format");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String validateUnique(UUID fieldId, Object value, UUID entryId, ValidationRule rule) {
        if (value == null) return null;
        boolean duplicate = fieldValueRepository.existsByFieldDefinitionIdAndValueTextAndEntryIdNot(
                fieldId, value.toString(), entryId != null ? entryId : UUID.fromString("00000000-0000-0000-0000-000000000000"));
        if (duplicate) {
            return message(rule, "This value already exists — must be unique");
        }
        return null;
    }

    // -------------------------------------------------------------------------

    private BigDecimal getParam(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) return null;
        try {
            return new BigDecimal(params.get(key).toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String message(ValidationRule rule, String fallback) {
        return (rule.getErrorMessage() != null && !rule.getErrorMessage().isBlank())
                ? rule.getErrorMessage()
                : fallback;
    }
}
