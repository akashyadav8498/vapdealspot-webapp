package com.vapedealspot.cms.dto;

public class CmsValidationIssue {

    private String path;
    private String message;

    public CmsValidationIssue() {
    }

    public CmsValidationIssue(String path, String message) {
        this.path = path;
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return path != null && !path.isEmpty() ? path + ": " + message : message;
    }
}

