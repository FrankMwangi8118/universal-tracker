package com.codify.universaltracker.validation.entity;

import com.codify.universaltracker.validation.converter.ValidationRuleTypeConverter;
import com.codify.universaltracker.validation.enums.ValidationRuleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "validation_rule")
@Getter
@Setter
public class ValidationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "field_definition_id", nullable = false)
    private UUID fieldDefinitionId;

    @Convert(converter = ValidationRuleTypeConverter.class)
    @Column(name = "rule_type", nullable = false, columnDefinition = "validation_rule_type")
    private ValidationRuleType ruleType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_params", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> ruleParams;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
