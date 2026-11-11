package com.vapedealspot.storefront.dto;

public class CategoryItemDto {
    private String id;
    private String slug;
    private String name;
    private String icon;
    private String path;
    private long count;

    public CategoryItemDto() {
    }

    public CategoryItemDto(String id, String slug, String name, String icon, String path, long count) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.icon = icon;
        this.path = path;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

