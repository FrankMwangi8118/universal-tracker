package com.codify.universaltracker.automation.entity;

import com.codify.universaltracker.automation.converter.AutomationActionConverter;
import com.codify.universaltracker.automation.converter.AutomationTriggerConverter;
import com.codify.universaltracker.automation.enums.AutomationAction;
import com.codify.universaltracker.automation.enums.AutomationTrigger;
import com.codify.universaltracker.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "automation")
@Getter
@Setter
public class Automation extends BaseEntity {

    @Column(name = "tracker_id", nullable = false)
    private UUID trackerId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Convert(converter = AutomationTriggerConverter.class)
    @Column(name = "trigger_event", nullable = false, columnDefinition = "automation_trigger")
    private AutomationTrigger triggerEvent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_config", columnDefinition = "jsonb")
    private Map<String, Object> triggerConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    private List<Map<String, Object>> conditions;

    @Convert(converter = AutomationActionConverter.class)
    @Column(name = "action_type", nullable = false, columnDefinition = "automation_action")
    private AutomationAction actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_params", columnDefinition = "jsonb")
    private Map<String, Object> actionParams;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_triggered")
    private Instant lastTriggered;

    @Column(name = "run_count")
    private Integer runCount = 0;
}
