package com.codify.universaltracker.field.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "field_option")
@Getter
@Setter
public class FieldOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "field_definition_id", nullable = false)
    private UUID fieldDefinitionId;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "value", nullable = false, length = 200)
    private String value;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
