package com.vapedealspot.cms.dto;

public class ProductDisplayDto {

    private String id;
    private String slug;
    private String brand;
    private String name;
    private String image;
    private Double minPrice;
    private Double maxPrice;
    private Integer shopCount;
    private String savingsBadge;
    private boolean inStock = true;

    public ProductDisplayDto() {
    }

    public ProductDisplayDto(String id, String slug, String brand, String name, String image,
                             Double minPrice, Double maxPrice, Integer shopCount, String savingsBadge) {
        this.id = id;
        this.slug = slug;
        this.brand = brand;
        this.name = name;
        this.image = image;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.shopCount = shopCount;
        this.savingsBadge = savingsBadge;
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

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getShopCount() {
        return shopCount;
    }

    public void setShopCount(Integer shopCount) {
        this.shopCount = shopCount;
    }

    public String getSavingsBadge() {
        return savingsBadge;
    }

    public void setSavingsBadge(String savingsBadge) {
        this.savingsBadge = savingsBadge;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }
}

