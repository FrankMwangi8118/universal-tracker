package com.codify.universaltracker.view.entity;

import com.codify.universaltracker.common.entity.BaseEntity;
import com.codify.universaltracker.view.enums.ViewType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "view_config")
@Getter
@Setter
public class ViewConfig extends BaseEntity {

    @Column(name = "tracker_id", nullable = false)
    private UUID trackerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Convert(converter = ViewTypeConverter.class)
    @Column(name = "view_type", nullable = false, columnDefinition = "view_type")
    private ViewType viewType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "columns", columnDefinition = "jsonb")
    private List<Map<String, Object>> columns;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sort_rules", columnDefinition = "jsonb")
    private List<Map<String, Object>> sortRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filter_rules", columnDefinition = "jsonb")
    private List<Map<String, Object>> filterRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "group_by", columnDefinition = "jsonb")
    private Map<String, Object> groupBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aggregations", columnDefinition = "jsonb")
    private List<Map<String, Object>> aggregations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chart_config", columnDefinition = "jsonb")
    private Map<String, Object> chartConfig;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
