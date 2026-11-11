package com.vapedealspot.product.dto;

import java.util.ArrayList;
import java.util.List;

public class ProductFormDto {
    private String id;
    private String name;
    private String slug;
    private String brand;
    private String category;
    private String sku;
    private String shortDescription;
    private String description;
    private String tags;
    private String image;
    private String status = "active";
    private boolean featured = false;
    private boolean trending = false;
    private boolean showOnHomepage = false;

    private List<VariantFormDto> variants = new ArrayList<>();
    private List<OfferFormDto> offers = new ArrayList<>();

    public ProductFormDto() {
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isTrending() {
        return trending;
    }

    public void setTrending(boolean trending) {
        this.trending = trending;
    }

    public boolean isShowOnHomepage() {
        return showOnHomepage;
    }

    public void setShowOnHomepage(boolean showOnHomepage) {
        this.showOnHomepage = showOnHomepage;
    }

    public List<VariantFormDto> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantFormDto> variants) {
        this.variants = variants != null ? variants : new ArrayList<>();
    }

    public List<OfferFormDto> getOffers() {
        return offers;
    }

    public void setOffers(List<OfferFormDto> offers) {
        this.offers = offers != null ? offers : new ArrayList<>();
    }
}

