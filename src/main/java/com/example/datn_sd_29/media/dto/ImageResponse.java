package com.example.datn_sd_29.media.dto;

import com.example.datn_sd_29.media.entity.Image;

public class ImageResponse {

    private Integer imageId;
    private String imageUrl;
    private Integer productId;
    private Integer productComboId;
    private Boolean isPrimary;

    public ImageResponse(Image image) {
        this.imageId = image.getId();
        this.imageUrl = image.getImageUrl();
        this.isPrimary = image.getIsPrimary();

        if (image.getProduct() != null) {
            this.productId = image.getProduct().getId();
        }

        if (image.getProductCombo() != null) {
            this.productComboId = image.getProductCombo().getId();
        }
    }

    public Integer getImageId() {
        return imageId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Integer getProductId() {
        return productId;
    }

    public Integer getProductComboId() {
        return productComboId;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }
}