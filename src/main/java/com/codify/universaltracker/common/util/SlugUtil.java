package com.codify.universaltracker.common.util;

import java.text.Normalizer;
import java.util.UUID;
import java.util.function.Predicate;

public final class SlugUtil {

    private SlugUtil() {}

    /**
     * Generates a URL-safe slug from any name.
     * "Savings Tracker" → "savings-tracker"
     */
    public static String toSlug(String name) {
        if (name == null || name.isBlank()) {
            return "untitled";
        }
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    /**
     * Generates a unique slug by appending a short random suffix if the
     * candidate slug already exists (checked via the provided predicate).
     */
    public static String toUniqueSlug(String name, Predicate<String> exists) {
        String base = toSlug(name);
        String candidate = base;

        if (!exists.test(candidate)) {
            return candidate;
        }

        // Append a 6-char random suffix derived from a UUID
        for (int i = 0; i < 10; i++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
            candidate = base + "-" + suffix;
            if (!exists.test(candidate)) {
                return candidate;
            }
        }

        // Fallback: full UUID suffix
        return base + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
