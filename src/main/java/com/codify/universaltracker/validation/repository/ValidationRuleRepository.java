package com.codify.universaltracker.validation.repository;

import com.codify.universaltracker.validation.entity.ValidationRule;
import com.codify.universaltracker.validation.enums.ValidationRuleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ValidationRuleRepository extends JpaRepository<ValidationRule, UUID> {

    List<ValidationRule> findByFieldDefinitionIdOrderByPriorityAsc(UUID fieldDefinitionId);

    List<ValidationRule> findByFieldDefinitionIdAndIsActiveTrueOrderByPriorityAsc(UUID fieldDefinitionId);

    boolean existsByFieldDefinitionIdAndRuleType(UUID fieldDefinitionId, ValidationRuleType ruleType);

    Optional<ValidationRule> findByFieldDefinitionIdAndRuleType(UUID fieldDefinitionId, ValidationRuleType ruleType);
}
