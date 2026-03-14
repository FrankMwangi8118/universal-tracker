package com.codify.universaltracker.field.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFieldOptionRequest(

        @NotBlank(message = "Label is required")
        @Size(max = 200)
        String label,

        @NotBlank(message = "Value is required")
        @Size(max = 200)
        String value,

        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color must be a valid hex code")
        String color,

        @Size(max = 50)
        String icon,

        Integer sortOrder,

        Boolean isDefault
) {}
