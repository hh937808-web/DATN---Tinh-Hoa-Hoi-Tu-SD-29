package com.example.datn_sd_29.media.service;

import com.example.datn_sd_29.media.dto.ImageResponse;
import com.example.datn_sd_29.media.entity.Image;
import com.example.datn_sd_29.media.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;


    public List<ImageResponse> getProductImages(Integer productId) {

        List<Image> images = imageRepository
                .findByProduct_IdOrderByIsPrimaryDesc(productId);

        return images.stream()
                .map(ImageResponse::new)
                .toList();
    }

    public ImageResponse getPrimaryProductImage(Integer productId) {

        Image image = imageRepository
                .findByProduct_IdAndIsPrimaryTrue(productId)
                .orElseThrow(() -> new RuntimeException("Primary image not found"));

        return new ImageResponse(image);
    }


    public List<ImageResponse> getProductComboImages(Integer comboId) {

        List<Image> images = imageRepository
                .findByProductCombo_IdOrderByIsPrimaryDesc(comboId);

        return images.stream()
                .map(ImageResponse::new)
                .toList();
    }

    public ImageResponse getPrimaryProductComboImage(Integer comboId) {

        Image image = imageRepository
                .findByProductCombo_IdAndIsPrimaryTrue(comboId)
                .orElseThrow(() -> new RuntimeException("Primary image not found"));

        return new ImageResponse(image);
    }
}