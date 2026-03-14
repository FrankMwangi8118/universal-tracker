package com.codify.universaltracker.domain.entity;

import com.codify.universaltracker.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "domain")
@Getter
@Setter
public class Domain extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "color", length = 7)
    private String color;

    @Column(name = "description")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
