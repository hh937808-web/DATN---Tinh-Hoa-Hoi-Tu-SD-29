package com.example.datn_sd_29.media.service;

import com.example.datn_sd_29.media.dto.ImageResponse;
import com.example.datn_sd_29.media.entity.Image;
import com.example.datn_sd_29.media.repository.ImageRepository;
import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product.repository.ProductRepository;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import com.example.datn_sd_29.product_combo.repository.ProductComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final ProductComboRepository productComboRepository;
    private final ProductRepository productRepository;


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

    public ImageResponse uploadComboImage(Integer comboId, MultipartFile file) throws IOException {
        
        ProductCombo combo = productComboRepository.findById(comboId)
                .orElseThrow(() -> new RuntimeException("Combo not found with id: " + comboId));

        // Set all existing images to non-primary
        List<Image> existingImages = imageRepository.findByProductCombo_IdOrderByIsPrimaryDesc(comboId);
        existingImages.forEach(img -> img.setIsPrimary(false));
        imageRepository.saveAll(existingImages);

        // Convert file to base64 data URL
        byte[] fileBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
        String contentType = file.getContentType();
        String dataUrl = "data:" + contentType + ";base64," + base64Image;

        // Create new image
        Image image = new Image();
        image.setProductCombo(combo);
        image.setImageUrl(dataUrl);
        image.setIsPrimary(true);

        Image savedImage = imageRepository.save(image);

        return new ImageResponse(savedImage);
    }
    
    public ImageResponse uploadProductImage(Integer productId, MultipartFile file) throws IOException {
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        // Set all existing images to non-primary
        List<Image> existingImages = imageRepository.findByProduct_IdOrderByIsPrimaryDesc(productId);
        existingImages.forEach(img -> img.setIsPrimary(false));
        imageRepository.saveAll(existingImages);

        // Convert file to base64 data URL
        byte[] fileBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
        String contentType = file.getContentType();
        String dataUrl = "data:" + contentType + ";base64," + base64Image;

        // Create new image
        Image image = new Image();
        image.setProduct(product);
        image.setImageUrl(dataUrl);
        image.setIsPrimary(true);

        Image savedImage = imageRepository.save(image);

        return new ImageResponse(savedImage);
    }
}