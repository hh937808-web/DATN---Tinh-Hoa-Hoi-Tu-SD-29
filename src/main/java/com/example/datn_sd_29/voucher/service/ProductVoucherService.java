package com.example.datn_sd_29.voucher.service;

import com.example.datn_sd_29.voucher.dto.ProductVoucherRequest;
import com.example.datn_sd_29.voucher.dto.ProductVoucherResponse;
import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.voucher.entity.ProductVoucher;
import com.example.datn_sd_29.product.repository.ProductRepository;
import com.example.datn_sd_29.voucher.repository.ProductVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVoucherService {

    private final ProductVoucherRepository productVoucherRepository;
    private final ProductRepository productRepository;
    private final VoucherCodeValidator voucherCodeValidator;

    // =====================
    // GET ALL
    // =====================
    public List<ProductVoucherResponse> getAll() {
        return productVoucherRepository.findAll()
                .stream()
                .map(ProductVoucherResponse::new)
                .toList();
    }

    // =====================
    // CREATE
    // =====================
    public ProductVoucherResponse create(ProductVoucherRequest request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy Product id = " + request.getProductId()
                ));

        // FIX #10: Validate discount percent must be 1-100%
        if (request.getDiscountPercent() != null) {
            if (request.getDiscountPercent() < 1 || request.getDiscountPercent() > 100) {
                throw new IllegalArgumentException("Giảm giá phải từ 1% đến 100%");
            }
        }

        // FIX #7: Validate date range
        if (request.getValidFrom() != null && request.getValidTo() != null) {
            if (request.getValidTo().isBefore(request.getValidFrom())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
            }
        }

        ProductVoucher voucher = new ProductVoucher();
        
        // Voucher code generation logic:
        // 1. If admin provides code -> use it
        // 2. If code is "AUTO" -> generate based on product ID (P{productId})
        // 3. If code is "RANDOM" -> generate random code
        // 4. If code is empty/null -> generate random code (default)
        String voucherCode = request.getVoucherCode();
        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            voucherCode = generateRandomVoucherCode();
        } else if ("AUTO".equalsIgnoreCase(voucherCode.trim())) {
            voucherCode = generateProductVoucherCode(request.getProductId());
        } else if ("RANDOM".equalsIgnoreCase(voucherCode.trim())) {
            voucherCode = generateRandomVoucherCode();
        }
        // else: use admin's custom code as-is
        
        // FIX #19: Check if voucher code already exists (across all voucher types)
        voucherCodeValidator.validateUniqueVoucherCode(voucherCode);
        
        voucher.setVoucherCode(voucherCode);
        voucher.setVoucherName(request.getVoucherName());
        voucher.setDiscountPercent(request.getDiscountPercent());
        voucher.setProduct(product);
        voucher.setRemainingQuantity(request.getRemainingQuantity());
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidTo(request.getValidTo());
        voucher.setIsActive(true);
        voucher.setCreatedAt(Instant.now());

        ProductVoucher saved = productVoucherRepository.save(voucher);

        return new ProductVoucherResponse(saved);
    }
    
    /**
     * Generate voucher code based on product ID
     * Format: P{productId}
     * Example: Product ID 1003 -> P1003
     */
    private String generateProductVoucherCode(Integer productId) {
        return "P" + productId;
    }
    
    /**
     * Generate random voucher code
     * Format: 8 uppercase alphanumeric characters
     * Example: VCH8X2K9, A7B3C9D2
     */
    private String generateRandomVoucherCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(8);
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    public ProductVoucherResponse update(Integer id, ProductVoucherRequest request) {

        ProductVoucher voucher = productVoucherRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("ProductVoucher not found with id: " + id)
                );

        // FIX #10: Validate discount percent must be 1-100%
        if (request.getDiscountPercent() != null) {
            if (request.getDiscountPercent() < 1 || request.getDiscountPercent() > 100) {
                throw new IllegalArgumentException("Giảm giá phải từ 1% đến 100%");
            }
        }

        // FIX #7: Validate date range
        if (request.getValidFrom() != null && request.getValidTo() != null) {
            if (request.getValidTo().isBefore(request.getValidFrom())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
            }
        }

        // FIX #8: REMOVED - Allow admin to deactivate voucher anytime
        // Admin có quyền vô hiệu hóa voucher bất cứ lúc nào (đồng nhất với CustomerVoucher)

        voucher.setVoucherCode(request.getVoucherCode());
        voucher.setVoucherName(request.getVoucherName());
        voucher.setDiscountPercent(request.getDiscountPercent());
        
        // FIX #9: Validate remaining_quantity and auto-update status
        if (request.getRemainingQuantity() != null) {
            if (request.getRemainingQuantity() < 0) {
                throw new IllegalArgumentException("Số lượt sử dụng không được âm");
            }
            voucher.setRemainingQuantity(request.getRemainingQuantity());
            
            // Auto-update status when remaining_quantity = 0
            if (request.getRemainingQuantity() == 0) {
                voucher.setIsActive(false);
            }
        }
        
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidTo(request.getValidTo());

        // Allow admin to manually activate voucher (if not used up)
        Boolean newStatus = request.getIsActive();
        if (!voucher.getIsActive() && Boolean.TRUE.equals(newStatus)) {
            // Only allow activation if voucher still has remaining uses
            if (voucher.getRemainingQuantity() != null && voucher.getRemainingQuantity() > 0) {
                voucher.setIsActive(true);
            } else {
                throw new IllegalArgumentException("Không thể kích hoạt voucher đã hết lượt sử dụng");
            }
        }
        
        // Allow admin to manually deactivate voucher
        if (voucher.getIsActive() && Boolean.FALSE.equals(newStatus)) {
            voucher.setIsActive(false);
        }

        return new ProductVoucherResponse(
                productVoucherRepository.save(voucher)
        );
    }



    public void delete(Integer id) {

        ProductVoucher voucher = productVoucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy ProductVoucher id = " + id
                ));

        voucher.setIsActive(false);
        productVoucherRepository.save(voucher);
    }
}
