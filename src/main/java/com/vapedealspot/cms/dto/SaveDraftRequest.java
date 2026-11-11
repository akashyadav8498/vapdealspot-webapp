package com.vapedealspot.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveDraftRequest {

    private PageDocumentDto document;
    private Integer expectedVersion;

    public SaveDraftRequest() {
    }

    public PageDocumentDto getDocument() {
        return document;
    }

    public void setDocument(PageDocumentDto document) {
        this.document = document;
    }

    public Integer getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Integer expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}

