package com.example.datn_sd_29.voucher.scheduler;

import com.example.datn_sd_29.voucher.repository.CustomerVoucherRepository;
import com.example.datn_sd_29.voucher.repository.ProductVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class VoucherExpirationScheduler {

    private final CustomerVoucherRepository customerVoucherRepository;
    private final ProductVoucherRepository productVoucherRepository;

    /**
     * Chạy mỗi ngày lúc 00:05 để kiểm tra và cập nhật trạng thái voucher hết hạn
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void updateExpiredVouchers() {
        log.info("Bắt đầu kiểm tra voucher hết hạn...");
        
        LocalDate today = LocalDate.now();
        
        // Cập nhật CustomerVoucher (voucher hóa đơn cá nhân)
        int expiredCustomerVouchers = customerVoucherRepository.updateExpiredVouchers(today);
        log.info("Đã cập nhật {} CustomerVoucher hết hạn", expiredCustomerVouchers);
        
        int usedUpCustomerVouchers = customerVoucherRepository.updateUsedUpVouchers();
        log.info("Đã cập nhật {} CustomerVoucher hết lượt sử dụng", usedUpCustomerVouchers);
        
        // Cập nhật ProductVoucher (voucher sản phẩm)
        int expiredProductVouchers = productVoucherRepository.updateExpiredProductVouchers(today);
        log.info("Đã vô hiệu hóa {} ProductVoucher hết hạn", expiredProductVouchers);
        
        int usedUpProductVouchers = productVoucherRepository.updateUsedUpProductVouchers();
        log.info("Đã vô hiệu hóa {} ProductVoucher hết số lượng", usedUpProductVouchers);
        
        log.info("Hoàn thành kiểm tra voucher hết hạn");
    }
    
    /**
     * Chạy ngay khi khởi động ứng dụng để cập nhật voucher hết hạn
     */
    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    @Transactional
    public void updateExpiredVouchersOnStartup() {
        log.info("Kiểm tra voucher hết hạn khi khởi động...");
        
        LocalDate today = LocalDate.now();
        
        // Cập nhật CustomerVoucher
        int expiredCustomerVouchers = customerVoucherRepository.updateExpiredVouchers(today);
        log.info("Đã cập nhật {} CustomerVoucher hết hạn khi khởi động", expiredCustomerVouchers);
        
        int usedUpCustomerVouchers = customerVoucherRepository.updateUsedUpVouchers();
        log.info("Đã cập nhật {} CustomerVoucher hết lượt sử dụng khi khởi động", usedUpCustomerVouchers);
        
        // Cập nhật ProductVoucher
        int expiredProductVouchers = productVoucherRepository.updateExpiredProductVouchers(today);
        log.info("Đã vô hiệu hóa {} ProductVoucher hết hạn khi khởi động", expiredProductVouchers);
        
        int usedUpProductVouchers = productVoucherRepository.updateUsedUpProductVouchers();
        log.info("Đã vô hiệu hóa {} ProductVoucher hết số lượng khi khởi động", usedUpProductVouchers);
        
        log.info("Hoàn thành kiểm tra voucher hết hạn khi khởi động");
    }
}
