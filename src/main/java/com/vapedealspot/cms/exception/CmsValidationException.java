package com.vapedealspot.cms.exception;

import com.vapedealspot.cms.dto.CmsValidationIssue;

import java.util.ArrayList;
import java.util.List;

public class CmsValidationException extends CmsException {

    private final List<CmsValidationIssue> issues;

    public CmsValidationException(String message, List<CmsValidationIssue> issues) {
        super(message, 400);
        this.issues = issues != null ? issues : new ArrayList<>();
    }

    public List<CmsValidationIssue> getIssues() {
        return issues;
    }
}

