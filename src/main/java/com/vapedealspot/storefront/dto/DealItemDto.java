package com.vapedealspot.storefront.dto;

public class DealItemDto {
    private String id;
    private String merchantName;
    private String title;
    private String discountBadge;
    private String code;
    private boolean verified;
    private String expiry;
    private String affiliateUrl;

    public DealItemDto() {
    }

    public DealItemDto(String id, String merchantName, String title, String discountBadge,
                       String code, boolean verified, String expiry, String affiliateUrl) {
        this.id = id;
        this.merchantName = merchantName;
        this.title = title;
        this.discountBadge = discountBadge;
        this.code = code;
        this.verified = verified;
        this.expiry = expiry;
        this.affiliateUrl = affiliateUrl;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDiscountBadge() {
        return discountBadge;
    }

    public String getDiscountValue() {
        return discountBadge;
    }

    public void setDiscountBadge(String discountBadge) {
        this.discountBadge = discountBadge;
    }

    public void setDiscountValue(String discountValue) {
        this.discountBadge = discountValue;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getExpiry() {
        return expiry;
    }

    public String getExpiryNotice() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public void setExpiryNotice(String expiryNotice) {
        this.expiry = expiryNotice;
    }

    public String getAffiliateUrl() {
        return affiliateUrl;
    }

    public void setAffiliateUrl(String affiliateUrl) {
        this.affiliateUrl = affiliateUrl;
    }
}

