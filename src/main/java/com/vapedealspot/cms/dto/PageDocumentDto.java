package com.vapedealspot.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageDocumentDto {

    private String id;
    private String title;
    private String slug;
    private Integer schemaVersion = 1;
    private Integer version = 1;
    private PageSeoMetadataDto seo;
    private List<PageSectionDto> sections = new ArrayList<>();
    private String updatedAt;
    private String publishedAt;

    public PageDocumentDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public PageSeoMetadataDto getSeo() {
        return seo;
    }

    public void setSeo(PageSeoMetadataDto seo) {
        this.seo = seo;
    }

    public List<PageSectionDto> getSections() {
        return sections;
    }

    public void setSections(List<PageSectionDto> sections) {
        this.sections = sections != null ? sections : new ArrayList<>();
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }
}

