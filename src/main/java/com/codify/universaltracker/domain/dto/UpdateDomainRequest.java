package com.codify.universaltracker.domain.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDomainRequest(

        @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
        String name,

        @Size(max = 50)
        String icon,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color must be a valid hex code")
        String color,

        String description,

        Integer sortOrder,

        Boolean isActive
) {}
