package com.codify.universaltracker.tracker.entity;

import com.codify.universaltracker.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tracker")
@Getter
@Setter
public class Tracker extends BaseEntity {

    @Column(name = "domain_id", nullable = false)
    private UUID domainId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "description")
    private String description;

    @Column(name = "entry_name_singular", length = 50)
    private String entryNameSingular = "Entry";

    @Column(name = "entry_name_plural", length = 50)
    private String entryNamePlural = "Entries";

    @Column(name = "default_date_field", length = 100)
    private String defaultDateField;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_config", columnDefinition = "jsonb")
    private Map<String, Object> summaryConfig;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
