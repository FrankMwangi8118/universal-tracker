package com.codify.universaltracker.entry.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "field_value")
@Getter
@Setter
public class FieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "entry_id", nullable = false)
    private UUID entryId;

    @Column(name = "field_definition_id", nullable = false)
    private UUID fieldDefinitionId;

    @Column(name = "value_text")
    private String valueText;

    @Column(name = "value_number", precision = 15, scale = 4)
    private BigDecimal valueNumber;

    @Column(name = "value_boolean")
    private Boolean valueBoolean;

    @Column(name = "value_date")
    private Instant valueDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", columnDefinition = "jsonb")
    private Object valueJson;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
