package com.example.datn_sd_29.media.service;

import com.example.datn_sd_29.media.dto.ImageResponse;
import com.example.datn_sd_29.media.entity.Image;
import com.example.datn_sd_29.media.repository.ImageRepository;
import com.example.datn_sd_29.blog.entity.BlogPost;
import com.example.datn_sd_29.blog.repository.BlogPostRepository;
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
    private final BlogPostRepository blogPostRepository;


    public List<ImageResponse> getProductImages(Integer productId) {

        List<Image> images = imageRepository
                .findByProduct_IdOrderByIsPrimaryDesc(productId);

        return images.stream()
                .map(ImageResponse::new)
                .toList();
    }

    public ImageResponse getPrimaryProductImage(Integer productId) {

        List<Image> images = imageRepository
                .findByProduct_IdAndIsPrimaryTrue(productId);
        if (images.isEmpty()) {
            throw new RuntimeException("Primary image not found");
        }

        return new ImageResponse(images.get(0));
    }


    public List<ImageResponse> getProductComboImages(Integer comboId) {

        List<Image> images = imageRepository
                .findByProductCombo_IdOrderByIsPrimaryDesc(comboId);

        return images.stream()
                .map(ImageResponse::new)
                .toList();
    }

    public ImageResponse getPrimaryProductComboImage(Integer comboId) {

        List<Image> images = imageRepository
                .findByProductCombo_IdAndIsPrimaryTrue(comboId);
        if (images.isEmpty()) {
            throw new RuntimeException("Primary image not found");
        }

        return new ImageResponse(images.get(0));
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

    // ===== BLOG POST IMAGES =====

    public List<ImageResponse> getBlogPostImages(Integer blogPostId) {
        return imageRepository.findByBlogPost_IdOrderByIsPrimaryDesc(blogPostId)
                .stream()
                .map(ImageResponse::new)
                .toList();
    }

    public ImageResponse uploadBlogPostImage(Integer blogPostId, MultipartFile file, boolean isPrimary) throws IOException {
        BlogPost blogPost = blogPostRepository.findById(blogPostId)
                .orElseThrow(() -> new RuntimeException("Blog post not found with id: " + blogPostId));

        if (isPrimary) {
            List<Image> existingImages = imageRepository.findByBlogPost_IdOrderByIsPrimaryDesc(blogPostId);
            existingImages.forEach(img -> img.setIsPrimary(false));
            imageRepository.saveAll(existingImages);
        }

        byte[] fileBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
        String contentType = file.getContentType();
        String dataUrl = "data:" + contentType + ";base64," + base64Image;

        Image image = new Image();
        image.setBlogPost(blogPost);
        image.setImageUrl(dataUrl);
        image.setIsPrimary(isPrimary);

        return new ImageResponse(imageRepository.save(image));
    }

    public void deleteBlogPostImages(Integer blogPostId) {
        List<Image> images = imageRepository.findByBlogPost_IdOrderByIsPrimaryDesc(blogPostId);
        imageRepository.deleteAll(images);
    }
}