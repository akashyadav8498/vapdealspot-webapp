package com.vapedealspot.product.dto;

import java.math.BigDecimal;

public class VariantFormDto {
    private String id;
    private String sku;
    private String name;
    private String attributeName = "Color";
    private String attributeValue;
    private BigDecimal price;
    private Integer stock = 0;
    private String image;
    private String status = "active";

    public VariantFormDto() {
    }

    public VariantFormDto(String id, String sku, String name, String attributeName, String attributeValue,
                          BigDecimal price, Integer stock, String image, String status) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
        this.price = price;
        this.stock = stock;
        this.image = image;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

