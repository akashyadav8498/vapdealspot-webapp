package com.vapedealspot.product.dto;

import java.math.BigDecimal;

public class OfferFormDto {
    private String id;
    private String merchantName;
    private String merchantLogo;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String affiliateUrl;
    private String availability = "in_stock";
    private String shippingInfo;
    private String status = "active";

    public OfferFormDto() {
    }

    public OfferFormDto(String id, String merchantName, String merchantLogo, BigDecimal price,
                        BigDecimal originalPrice, String affiliateUrl, String availability,
                        String shippingInfo, String status) {
        this.id = id;
        this.merchantName = merchantName;
        this.merchantLogo = merchantLogo;
        this.price = price;
        this.originalPrice = originalPrice;
        this.affiliateUrl = affiliateUrl;
        this.availability = availability != null ? availability : "in_stock";
        this.shippingInfo = shippingInfo;
        this.status = status != null ? status : "active";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getMerchantLogo() {
        return merchantLogo;
    }

    public void setMerchantLogo(String merchantLogo) {
        this.merchantLogo = merchantLogo;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getAffiliateUrl() {
        return affiliateUrl;
    }

    public void setAffiliateUrl(String affiliateUrl) {
        this.affiliateUrl = affiliateUrl;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getShippingInfo() {
        return shippingInfo;
    }

    public void setShippingInfo(String shippingInfo) {
        this.shippingInfo = shippingInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

