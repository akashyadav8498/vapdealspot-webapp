package com.vapedealspot.cms.exception;

public class CmsForbiddenException extends CmsException {
    public CmsForbiddenException(String message) {
        super(message, 403);
    }
}

