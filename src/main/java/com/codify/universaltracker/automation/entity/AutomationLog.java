package com.codify.universaltracker.automation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "automation_log")
@Getter
@Setter
public class AutomationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "automation_id", nullable = false)
    private UUID automationId;

    @Column(name = "entry_id")
    private UUID entryId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_data", columnDefinition = "jsonb")
    private Map<String, Object> triggerData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_result", columnDefinition = "jsonb")
    private Map<String, Object> actionResult;

    @Column(name = "status", length = 20)
    private String status = "success";

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "executed_at", updatable = false)
    private Instant executedAt;

    @PrePersist
    protected void onCreate() {
        this.executedAt = Instant.now();
    }
}
