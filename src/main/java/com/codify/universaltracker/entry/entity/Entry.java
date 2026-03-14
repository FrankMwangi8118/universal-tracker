package com.codify.universaltracker.entry.entity;

import com.codify.universaltracker.common.entity.BaseEntity;
import com.codify.universaltracker.entry.converter.EntryStatusConverter;
import com.codify.universaltracker.entry.enums.EntryStatus;
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
@Table(name = "entry")
@Getter
@Setter
public class Entry extends BaseEntity {

    @Column(name = "tracker_id", nullable = false)
    private UUID trackerId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "entry_date", nullable = false)
    private Instant entryDate;

    @Convert(converter = EntryStatusConverter.class)
    @Column(name = "status", columnDefinition = "entry_status")
    private EntryStatus status = EntryStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags;

    @Column(name = "notes")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
