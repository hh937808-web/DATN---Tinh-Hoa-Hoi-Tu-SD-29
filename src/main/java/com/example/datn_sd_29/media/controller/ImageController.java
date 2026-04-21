package com.example.datn_sd_29.media.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.media.dto.ImageResponse;
import com.example.datn_sd_29.media.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getProductImages(
            @PathVariable Integer productId
    ) {

        List<ImageResponse> images = imageService.getProductImages(productId);

        return ResponseEntity.ok(
                ApiResponse.success("Get product images successfully", images)
        );
    }

    @GetMapping("/product/{productId}/primary")
    public ResponseEntity<ApiResponse<ImageResponse>> getPrimaryProductImage(
            @PathVariable Integer productId
    ) {

        ImageResponse image = imageService.getPrimaryProductImage(productId);

        return ResponseEntity.ok(
                ApiResponse.success("Get primary image successfully", image)
        );
    }



    @GetMapping("/combo/{comboId}")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getComboImages(
            @PathVariable Integer comboId
    ) {

        List<ImageResponse> images = imageService.getProductComboImages(comboId);

        return ResponseEntity.ok(
                ApiResponse.success("Get combo images successfully", images)
        );
    }

    @GetMapping("/combo/{comboId}/primary")
    public ResponseEntity<ApiResponse<ImageResponse>> getPrimaryComboImage(
            @PathVariable Integer comboId
    ) {

        ImageResponse image = imageService.getPrimaryProductComboImage(comboId);

        return ResponseEntity.ok(
                ApiResponse.success("Get primary combo image successfully", image)
        );
    }

    @PostMapping("/combo/{comboId}")
    public ResponseEntity<ApiResponse<ImageResponse>> uploadComboImage(
            @PathVariable Integer comboId,
            @RequestParam("image") MultipartFile file
    ) {
        try {
            ImageResponse image = imageService.uploadComboImage(comboId, file);

            return ResponseEntity.ok(
                    ApiResponse.success("Upload combo image successfully", image)
            );
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to upload image: " + e.getMessage(), null)
            );
        }
    }
    
    @PostMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<ImageResponse>> uploadProductImage(
            @PathVariable Integer productId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            ImageResponse image = imageService.uploadProductImage(productId, file);

            return ResponseEntity.ok(
                    ApiResponse.success("Upload product image successfully", image)
            );
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to upload image: " + e.getMessage(), null)
            );
        }
    }

    // ===== BLOG POST IMAGES =====

    @GetMapping("/blog/{blogPostId}")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getBlogPostImages(
            @PathVariable Integer blogPostId
    ) {
        List<ImageResponse> images = imageService.getBlogPostImages(blogPostId);
        return ResponseEntity.ok(ApiResponse.success("Get blog images successfully", images));
    }

    @PostMapping("/blog/{blogPostId}")
    public ResponseEntity<ApiResponse<ImageResponse>> uploadBlogPostImage(
            @PathVariable Integer blogPostId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary
    ) {
        try {
            ImageResponse image = imageService.uploadBlogPostImage(blogPostId, file, isPrimary);
            return ResponseEntity.ok(ApiResponse.success("Upload blog image successfully", image));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Failed to upload image: " + e.getMessage(), null)
            );
        }
    }

    @DeleteMapping("/blog/{blogPostId}")
    public ResponseEntity<ApiResponse<Void>> deleteBlogPostImages(@PathVariable Integer blogPostId) {
        imageService.deleteBlogPostImages(blogPostId);
        return ResponseEntity.ok(ApiResponse.success("Deleted blog images", null));
    }
}