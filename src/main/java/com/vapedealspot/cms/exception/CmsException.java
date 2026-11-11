package com.vapedealspot.cms.exception;

public class CmsException extends RuntimeException {

    private final int statusCode;

    public CmsException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CmsException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

