package com.example.datn_sd_29.media.controller;

import com.example.datn_sd_29.common.dto.ApiResponse;
import com.example.datn_sd_29.media.dto.ImageResponse;
import com.example.datn_sd_29.media.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}