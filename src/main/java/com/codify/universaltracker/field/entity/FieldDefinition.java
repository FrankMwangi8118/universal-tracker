package com.codify.universaltracker.field.entity;

import com.codify.universaltracker.common.entity.BaseEntity;
import com.codify.universaltracker.field.enums.FieldType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "field_definition")
@Getter
@Setter
public class FieldDefinition extends BaseEntity {

    @Column(name = "tracker_id", nullable = false)
    private UUID trackerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Convert(converter = FieldTypeConverter.class)
    @Column(name = "field_type", nullable = false, columnDefinition = "field_type")
    private FieldType fieldType;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "is_unique")
    private Boolean isUnique = false;

    @Column(name = "is_filterable")
    private Boolean isFilterable = true;

    @Column(name = "is_summable")
    private Boolean isSummable = false;

    @Column(name = "is_primary_display")
    private Boolean isPrimaryDisplay = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_value", columnDefinition = "jsonb")
    private Object defaultValue;

    @Column(name = "display_format", length = 100)
    private String displayFormat;

    @Column(name = "placeholder", length = 200)
    private String placeholder;

    @Column(name = "help_text")
    private String helpText;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "min_value", precision = 15, scale = 4)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 15, scale = 4)
    private BigDecimal maxValue;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditional_logic", columnDefinition = "jsonb")
    private Map<String, Object> conditionalLogic;

    @Column(name = "formula_expression")
    private String formulaExpression;
}
