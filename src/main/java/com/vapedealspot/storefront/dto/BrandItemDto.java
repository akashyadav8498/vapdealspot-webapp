package com.vapedealspot.storefront.dto;

public class BrandItemDto {
    private String id;
    private String slug;
    private String name;
    private String description;
    private String logoUrl;
    private long count;

    public BrandItemDto() {
    }

    public BrandItemDto(String id, String slug, String name, String description, String logoUrl, long count) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.description = description;
        this.logoUrl = logoUrl;
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public long getCount() {
        return count;
    }

    public long getProductCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}

