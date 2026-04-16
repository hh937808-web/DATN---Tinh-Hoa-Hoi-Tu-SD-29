package com.example.datn_sd_29.voucher.repository;

import com.example.datn_sd_29.voucher.entity.CustomerVoucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Integer> {
    List<CustomerVoucher> findByCustomerIdAndVoucherStatus(Integer customerId, String voucherStatus);

    List<CustomerVoucher> findByCustomerId(Integer customerId);
    
    // Sắp xếp: HOAT_DONG trước, sau đó theo ngày tạo mới nhất
    @Query("SELECT cv FROM CustomerVoucher cv ORDER BY " +
           "CASE WHEN cv.voucherStatus = 'HOAT_DONG' THEN 0 ELSE 1 END, " +
           "cv.createdAt DESC")
    List<CustomerVoucher> findAllOrderedByStatusAndCreatedAt();

    // FIX #6: Add pessimistic lock to prevent race condition when multiple users use same voucher
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerVoucher> findByIdAndCustomerId(Integer id, Integer customerId);
    
    // Find public vouchers (customer_id = NULL) that apply to all customers
    @Query("SELECT cv FROM CustomerVoucher cv WHERE cv.customer IS NULL")
    List<CustomerVoucher> findPublicVouchers();
    
    /**
     * Cập nhật trạng thái voucher hết hạn
     * @param currentDate Ngày hiện tại
     * @return Số lượng voucher đã được cập nhật
     */
    @Modifying
    @Query(value = "UPDATE CustomerVoucher SET voucher_status = 'HET_HAN' " +
           "WHERE expires_at < :currentDate " +
           "AND voucher_status IN ('HOAT_DONG', 'ACTIVE')", 
           nativeQuery = true)
    int updateExpiredVouchers(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Cập nhật trạng thái voucher hết lượt sử dụng
     * @return Số lượng voucher đã được cập nhật
     */
    @Modifying
    @Query(value = "UPDATE CustomerVoucher SET voucher_status = 'DA_DUNG' " +
           "WHERE remaining_quantity <= 0 " +
           "AND voucher_status = 'HOAT_DONG'", 
           nativeQuery = true)
    int updateUsedUpVouchers();
}
