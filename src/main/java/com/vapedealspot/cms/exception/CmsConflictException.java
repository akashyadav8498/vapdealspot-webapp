package com.vapedealspot.cms.exception;

public class CmsConflictException extends CmsException {
    public CmsConflictException(String message) {
        super(message, 409);
    }
}

