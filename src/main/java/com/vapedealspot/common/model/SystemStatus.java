package com.vapedealspot.common.model;

/**
 * Modern Java Sealed Interface modeling system operational status hierarchies.
 */
public sealed interface SystemStatus permits SystemStatus.Operational, SystemStatus.Degraded, SystemStatus.Maintenance {

    String message();

    record Operational(String message, String databaseResponseTimeMs) implements SystemStatus {}

    record Degraded(String message, String warningReason) implements SystemStatus {}

    record Maintenance(String message, String scheduledEndTime) implements SystemStatus {}

    /**
     * Demonstrates Java 21+ Pattern Matching for switch expressions.
     */
    default String getBadgeCssClass() {
        return switch (this) {
            case Operational op -> "admin-badge-success";
            case Degraded deg -> "admin-badge-warning";
            case Maintenance maint -> "admin-badge-danger";
        };
    }
}
