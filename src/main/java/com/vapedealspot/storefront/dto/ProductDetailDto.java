package com.vapedealspot.storefront.dto;

import com.vapedealspot.product.ProductOffer;
import com.vapedealspot.product.ProductVariant;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailDto {
    private String id;
    private String slug;
    private String name;
    private String brand;
    private String category;
    private String categoryName;
    private String sku;
    private String shortDescription;
    private String description;
    private String image;
    private BigDecimal rating;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private int shopCount;
    private String savingsBadge;
    private boolean inStock;

    private ProductOffer bestOffer;
    private List<ProductOffer> offers = new ArrayList<>();
    private List<ProductVariant> variants = new ArrayList<>();
    private List<BreadcrumbItem> breadcrumbs = new ArrayList<>();

    public ProductDetailDto() {
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public int getShopCount() {
        return shopCount;
    }

    public void setShopCount(int shopCount) {
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

    public ProductOffer getBestOffer() {
        return bestOffer;
    }

    public void setBestOffer(ProductOffer bestOffer) {
        this.bestOffer = bestOffer;
    }

    public List<ProductOffer> getOffers() {
        return offers;
    }

    public void setOffers(List<ProductOffer> offers) {
        this.offers = offers != null ? offers : new ArrayList<>();
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants != null ? variants : new ArrayList<>();
    }

    public List<BreadcrumbItem> getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(List<BreadcrumbItem> breadcrumbs) {
        this.breadcrumbs = breadcrumbs != null ? breadcrumbs : new ArrayList<>();
    }
}

