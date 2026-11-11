package com.vapedealspot.storefront.dto;

public class BreadcrumbItem {
    private String label;
    private String path;

    public BreadcrumbItem() {
    }

    public BreadcrumbItem(String label) {
        this.label = label;
        this.path = null;
    }

    public BreadcrumbItem(String label, String path) {
        this.label = label;
        this.path = path;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

