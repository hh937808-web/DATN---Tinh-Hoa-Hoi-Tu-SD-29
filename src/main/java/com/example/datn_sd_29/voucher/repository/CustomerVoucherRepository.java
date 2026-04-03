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

    // FIX #6: Add pessimistic lock to prevent race condition when multiple users use same voucher
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerVoucher> findByIdAndCustomerId(Integer id, Integer customerId);
    
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
           "WHERE remaining_uses <= 0 " +
           "AND voucher_status = 'HOAT_DONG'", 
           nativeQuery = true)
    int updateUsedUpVouchers();
}
