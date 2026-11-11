package com.vapedealspot.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RestoreRequest {

    private String slug = "/";
    private Long revisionId;
    private Integer expectedDraftVersion;

    public RestoreRequest() {
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Long getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(Long revisionId) {
        this.revisionId = revisionId;
    }

    public Integer getExpectedDraftVersion() {
        return expectedDraftVersion;
    }

    public void setExpectedDraftVersion(Integer expectedDraftVersion) {
        this.expectedDraftVersion = expectedDraftVersion;
    }
}

