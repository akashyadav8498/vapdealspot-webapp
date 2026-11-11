package com.vapedealspot.cms.exception;

public class CmsNotFoundException extends CmsException {
    public CmsNotFoundException(String message) {
        super(message, 404);
    }
}

