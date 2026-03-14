package com.codify.universaltracker.validation.service;

import com.codify.universaltracker.common.exception.DuplicateResourceException;
import com.codify.universaltracker.common.exception.ResourceNotFoundException;
import com.codify.universaltracker.field.repository.FieldDefinitionRepository;
import com.codify.universaltracker.validation.dto.CreateValidationRuleRequest;
import com.codify.universaltracker.validation.dto.UpdateValidationRuleRequest;
import com.codify.universaltracker.validation.dto.ValidationRuleResponse;
import com.codify.universaltracker.validation.entity.ValidationRule;
import com.codify.universaltracker.validation.repository.ValidationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ValidationRuleService {

    private static final Logger log = LoggerFactory.getLogger(ValidationRuleService.class);

    private final ValidationRuleRepository ruleRepository;
    private final FieldDefinitionRepository fieldRepository;

    public ValidationRuleService(ValidationRuleRepository ruleRepository,
                                  FieldDefinitionRepository fieldRepository) {
        this.ruleRepository = ruleRepository;
        this.fieldRepository = fieldRepository;
    }

    @Transactional
    public ValidationRuleResponse addRule(UUID fieldId, CreateValidationRuleRequest request) {
        fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("FieldDefinition", fieldId));

        if (ruleRepository.existsByFieldDefinitionIdAndRuleType(fieldId, request.ruleType())) {
            throw new DuplicateResourceException(
                    "ValidationRule", "ruleType", request.ruleType().name());
        }

        ValidationRule rule = new ValidationRule();
        rule.setFieldDefinitionId(fieldId);
        rule.setRuleType(request.ruleType());
        rule.setRuleParams(request.ruleParams() != null ? request.ruleParams() : Map.of());
        rule.setErrorMessage(request.errorMessage());
        rule.setPriority(request.priority() != null ? request.priority() : 0);

        ValidationRule saved = ruleRepository.save(rule);
        log.info("Added {} rule to field {}", request.ruleType(), fieldId);
        return ValidationRuleResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ValidationRuleResponse> listRules(UUID fieldId) {
        fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("FieldDefinition", fieldId));
        return ruleRepository.findByFieldDefinitionIdOrderByPriorityAsc(fieldId)
                .stream().map(ValidationRuleResponse::from).toList();
    }

    @Transactional
    public ValidationRuleResponse updateRule(UUID ruleId, UpdateValidationRuleRequest request) {
        ValidationRule rule = findRule(ruleId);

        if (request.ruleParams() != null) rule.setRuleParams(request.ruleParams());
        if (request.errorMessage() != null) rule.setErrorMessage(request.errorMessage());
        if (request.priority() != null) rule.setPriority(request.priority());
        if (request.isActive() != null) rule.setIsActive(request.isActive());

        return ValidationRuleResponse.from(ruleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        ValidationRule rule = findRule(ruleId);
        ruleRepository.delete(rule);
        log.info("Deleted validation rule id={}", ruleId);
    }

    private ValidationRule findRule(UUID ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("ValidationRule", ruleId));
    }
}
