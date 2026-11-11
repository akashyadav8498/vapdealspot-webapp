package com.vapedealspot.user.model;

/**
 * Unified application user roles.
 * Exactly three roles supported across the application.
 */
public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    CUSTOMER;

    public static UserRole fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        for (UserRole r : values()) {
            if (r.name().equals(clean)) {
                return r;
            }
        }
        return null;
    }

    public String getDisplayName() {
        return switch (this) {
            case SUPER_ADMIN -> "Super Admin";
            case ADMIN -> "Admin";
            case CUSTOMER -> "Customer";
        };
    }
}

