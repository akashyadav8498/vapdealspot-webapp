package com.vapedealspot.common.dto;

import java.time.Instant;

/**
 * Modern Java Record representing immutable system diagnostics metadata.
 */
public record SystemInfoRecord(
    String applicationName,
    String javaVersion,
    String springBootVersion,
    String environmentProfile,
    Instant serverTime,
    long uptimeMillis,
    String databaseStatus,
    long totalMemoryMb,
    long freeMemoryMb
) {
    public long usedMemoryMb() {
        return totalMemoryMb - freeMemoryMb;
    }
}
