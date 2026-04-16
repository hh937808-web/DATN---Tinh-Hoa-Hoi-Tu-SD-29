package com.example.datn_sd_29.voucher.repository;

import com.example.datn_sd_29.voucher.entity.ProductVoucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductVoucherRepository extends JpaRepository<ProductVoucher, Integer> {
    List<ProductVoucher> findByIsActiveTrue();
    
    // Sắp xếp: Active trước, sau đó theo ngày tạo mới nhất
    @Query("SELECT pv FROM ProductVoucher pv ORDER BY pv.isActive DESC, pv.createdAt DESC")
    List<ProductVoucher> findAllOrderedByStatusAndCreatedAt();
    
    // FIX #6: Add pessimistic lock to prevent race condition when multiple users use same voucher
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductVoucher> findByVoucherCode(String voucherCode);
    
    // FIX #19: Check if voucher code already exists to prevent duplicates
    boolean existsByVoucherCode(String voucherCode);
    
    /**
     * Tự động vô hiệu hóa ProductVoucher đã hết hạn
     * @param currentDate Ngày hiện tại
     * @return Số lượng voucher đã được vô hiệu hóa
     */
    @Modifying
    @Query(value = "UPDATE ProductVoucher SET is_active = 0 " +
           "WHERE valid_to < :currentDate " +
           "AND is_active = 1", 
           nativeQuery = true)
    int updateExpiredProductVouchers(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Tự động vô hiệu hóa ProductVoucher hết số lượng
     * @return Số lượng voucher đã được vô hiệu hóa
     */
    @Modifying
    @Query(value = "UPDATE ProductVoucher SET is_active = 0 " +
           "WHERE remaining_quantity <= 0 " +
           "AND is_active = 1", 
           nativeQuery = true)
    int updateUsedUpProductVouchers();
    
    /**
     * Kiểm tra xem sản phẩm đã có voucher ACTIVE chưa
     * @param productId ID của sản phẩm
     * @return true nếu sản phẩm đã có voucher ACTIVE
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END " +
           "FROM ProductVoucher pv " +
           "WHERE pv.product.id = :productId AND pv.isActive = true")
    boolean existsActiveVoucherForProduct(@Param("productId") Integer productId);
    
    /**
     * Tìm voucher ACTIVE của sản phẩm (nếu có)
     * @param productId ID của sản phẩm
     * @return Optional chứa ProductVoucher nếu tìm thấy
     */
    @Query("SELECT pv FROM ProductVoucher pv " +
           "WHERE pv.product.id = :productId AND pv.isActive = true")
    Optional<ProductVoucher> findActiveVoucherByProductId(@Param("productId") Integer productId);
}
