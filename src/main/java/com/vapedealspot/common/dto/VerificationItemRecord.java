package com.vapedealspot.common.dto;

/**
 * Modern Java Record representing a foundation checklist verification item.
 */
public record VerificationItemRecord(
    String category,
    String name,
    String expected,
    String actual,
    boolean passed,
    String notes
) {}
