package com.vapedealspot.product.dto;

import java.math.BigDecimal;

public class ProductListItemDto {
    private String id;
    private String name;
    private String brand;
    private String category;
    private String sku;
    private String image;
    private BigDecimal lowestPrice;
    private int offersCount;
    private String status;
    private boolean featured;

    public ProductListItemDto() {
    }

    public ProductListItemDto(String id, String name, String brand, String category, String sku,
                              String image, BigDecimal lowestPrice, int offersCount, String status, boolean featured) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.sku = sku;
        this.image = image;
        this.lowestPrice = lowestPrice;
        this.offersCount = offersCount;
        this.status = status;
        this.featured = featured;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public BigDecimal getLowestPrice() {
        return lowestPrice;
    }

    public void setLowestPrice(BigDecimal lowestPrice) {
        this.lowestPrice = lowestPrice;
    }

    public int getOffersCount() {
        return offersCount;
    }

    public void setOffersCount(int offersCount) {
        this.offersCount = offersCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }
}

