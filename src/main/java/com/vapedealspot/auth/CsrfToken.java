package com.vapedealspot.auth;

/**
 * Lightweight CSRF token representation for Thymeleaf view rendering and form binding.
 */
public record CsrfToken(String token, String parameterName, String headerName) {

    public static final String DEFAULT_PARAM_NAME = "_csrf";
    public static final String DEFAULT_HEADER_NAME = "X-CSRF-TOKEN";

    public CsrfToken(String token) {
        this(token, DEFAULT_PARAM_NAME, DEFAULT_HEADER_NAME);
    }
}

