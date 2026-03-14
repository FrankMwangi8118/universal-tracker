package com.codify.universaltracker.field.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateFieldOptionRequest(

        @Size(min = 1, max = 200)
        String label,

        @Size(min = 1, max = 200)
        String value,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color must be a valid hex code")
        String color,

        @Size(max = 50)
        String icon,

        Integer sortOrder,

        Boolean isDefault,

        Boolean isActive
) {}
