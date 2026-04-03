package com.example.datn_sd_29.voucher.scheduler;

import com.example.datn_sd_29.voucher.repository.CustomerVoucherRepository;
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

    /**
     * Chạy mỗi ngày lúc 00:05 để kiểm tra và cập nhật trạng thái voucher hết hạn
     */
    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void updateExpiredVouchers() {
        log.info("Bắt đầu kiểm tra voucher hết hạn...");
        
        LocalDate today = LocalDate.now();
        
        // Tìm tất cả voucher đang hoạt động nhưng đã hết hạn
        int expiredCount = customerVoucherRepository.updateExpiredVouchers(today);
        
        log.info("Đã cập nhật {} voucher hết hạn", expiredCount);
        
        // Tìm tất cả voucher đang hoạt động nhưng đã hết lượt sử dụng
        int usedUpCount = customerVoucherRepository.updateUsedUpVouchers();
        
        log.info("Đã cập nhật {} voucher hết lượt sử dụng", usedUpCount);
    }
    
    /**
     * Chạy ngay khi khởi động ứng dụng để cập nhật voucher hết hạn
     */
    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    @Transactional
    public void updateExpiredVouchersOnStartup() {
        log.info("Kiểm tra voucher hết hạn khi khởi động...");
        
        LocalDate today = LocalDate.now();
        int expiredCount = customerVoucherRepository.updateExpiredVouchers(today);
        
        log.info("Đã cập nhật {} voucher hết hạn khi khởi động", expiredCount);
        
        // Kiểm tra voucher hết lượt sử dụng
        int usedUpCount = customerVoucherRepository.updateUsedUpVouchers();
        
        log.info("Đã cập nhật {} voucher hết lượt sử dụng khi khởi động", usedUpCount);
    }
}
