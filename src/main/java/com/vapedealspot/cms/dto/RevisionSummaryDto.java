package com.vapedealspot.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RevisionSummaryDto {

    private Long id;
    private String pageId;
    private Integer version;
    private Integer schemaVersion;
    private String publishedBy;
    private String createdAt;
    private String metaTitle;
    private String metaDescription;
    private String heroHeadline;
    private String heroTitlePrimary;
    private String heroTitleHighlight;
    private int sectionsCount;
    private List<String> enabledSections = new ArrayList<>();
    private PageDocumentDto document;

    public RevisionSummaryDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getMetaTitle() {
        return metaTitle;
    }

    public void setMetaTitle(String metaTitle) {
        this.metaTitle = metaTitle;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public String getHeroHeadline() {
        return heroHeadline;
    }

    public void setHeroHeadline(String heroHeadline) {
        this.heroHeadline = heroHeadline;
    }

    public String getHeroTitlePrimary() {
        return heroTitlePrimary;
    }

    public void setHeroTitlePrimary(String heroTitlePrimary) {
        this.heroTitlePrimary = heroTitlePrimary;
    }

    public String getHeroTitleHighlight() {
        return heroTitleHighlight;
    }

    public void setHeroTitleHighlight(String heroTitleHighlight) {
        this.heroTitleHighlight = heroTitleHighlight;
    }

    public int getSectionsCount() {
        return sectionsCount;
    }

    public void setSectionsCount(int sectionsCount) {
        this.sectionsCount = sectionsCount;
    }

    public List<String> getEnabledSections() {
        return enabledSections;
    }

    public void setEnabledSections(List<String> enabledSections) {
        this.enabledSections = enabledSections != null ? enabledSections : new ArrayList<>();
    }

    public PageDocumentDto getDocument() {
        return document;
    }

    public void setDocument(PageDocumentDto document) {
        this.document = document;
    }
}

