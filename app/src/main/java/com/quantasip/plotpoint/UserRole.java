package com.quantasip.plotpoint;

public enum UserRole {
    ADMIN,
    USER;

    // Utility method to convert String to UserRole
    public static UserRole fromString(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // Handle invalid or null role values
        }
    }
}

