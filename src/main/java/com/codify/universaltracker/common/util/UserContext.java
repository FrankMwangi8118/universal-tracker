package com.codify.universaltracker.common.util;

import com.codify.universaltracker.common.exception.BusinessRuleException;

import java.util.UUID;

public final class UserContext {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static UUID get() {
        UUID userId = CURRENT_USER.get();
        if (userId == null) {
            throw new BusinessRuleException("No authenticated user in context. Provide X-User-Id header.");
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
