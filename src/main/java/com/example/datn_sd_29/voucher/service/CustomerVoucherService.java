package com.example.datn_sd_29.voucher.service;

import com.example.datn_sd_29.voucher.dto.CustomerVoucherRequest;
import com.example.datn_sd_29.voucher.dto.CustomerVoucherResponse;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.voucher.entity.CustomerVoucher;
import com.example.datn_sd_29.voucher.entity.PersonalVoucher;
import com.example.datn_sd_29.voucher.enums.VoucherStatus;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.voucher.repository.CustomerVoucherRepository;
import com.example.datn_sd_29.voucher.repository.PersonalVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerVoucherService {

    private final CustomerVoucherRepository customerVoucherRepository;
    private final PersonalVoucherRepository personalVoucherRepository;
    private final CustomerRepository customerRepository;
    private final VoucherCodeValidator voucherCodeValidator;

    public List<CustomerVoucherResponse> getAll() {
        return customerVoucherRepository.findAllOrderedByStatusAndCreatedAt()
                .stream()
                .map(CustomerVoucherResponse::new)
                .toList();
    }

    public CustomerVoucherResponse create(CustomerVoucherRequest request) {

        // Validate: Either personalVoucherId OR (voucherCode + voucherName + discountPercent)
        boolean hasTemplate = request.getPersonalVoucherId() != null;
        boolean hasDirectInfo = request.getVoucherCode() != null && 
                                request.getVoucherName() != null && 
                                request.getDiscountPercent() != null;

        if (!hasTemplate && !hasDirectInfo) {
            throw new IllegalArgumentException(
                "Phải cung cấp Personal Voucher ID hoặc thông tin voucher (mã, tên, % giảm giá)"
            );
        }

        if (hasTemplate && hasDirectInfo) {
            throw new IllegalArgumentException(
                "Chỉ chọn một trong hai: từ template hoặc tạo trực tiếp"
            );
        }

        // FIX #10: Validate discount percent must be 1-100%
        if (hasDirectInfo && request.getDiscountPercent() != null) {
            if (request.getDiscountPercent() < 1 || request.getDiscountPercent() > 100) {
                throw new IllegalArgumentException("Giảm giá phải từ 1% đến 100%");
            }
        }

        PersonalVoucher personalVoucher;

        // Option 1: From template
        if (hasTemplate) {
            personalVoucher = personalVoucherRepository
                    .findById(request.getPersonalVoucherId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("PersonalVoucher not found with id: " + request.getPersonalVoucherId())
                    );
        } 
        // Option 2: Create new PersonalVoucher on-the-fly
        else {
            personalVoucher = new PersonalVoucher();
            personalVoucher.setVoucherCode(request.getVoucherCode());
            personalVoucher.setVoucherName(request.getVoucherName());
            personalVoucher.setDiscountPercent(request.getDiscountPercent());
            personalVoucher.setVoucherType("Voucher khách hàng");
            
            // FIX #19: Check if voucher code already exists (across all voucher types)
            voucherCodeValidator.validateUniqueVoucherCode(request.getVoucherCode());
            
            personalVoucher = personalVoucherRepository.save(personalVoucher);
        }

        // Customer is now optional - null means voucher applies to all customers
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository
                    .findById(request.getCustomerId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Customer not found with id: " + request.getCustomerId())
                    );
        }

        // FIX #5: Validate expiry date must be in the future
        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày hết hạn phải trong tương lai");
        }

        if (request.getExpiresAt() != null &&
                request.getIssuedAt() != null &&
                request.getExpiresAt().isBefore(request.getIssuedAt())) {
            throw new IllegalArgumentException("Ngày hết hạn phải sau ngày phát hành");
        }

        CustomerVoucher voucher = new CustomerVoucher();
        voucher.setPersonalVoucher(personalVoucher);
        voucher.setCustomer(customer);
        voucher.setIssuedAt(request.getIssuedAt());
        voucher.setExpiresAt(request.getExpiresAt());
        voucher.setRemainingQuantity(request.getRemainingQuantity());
        voucher.setVoucherStatus(VoucherStatus.HOAT_DONG);
        voucher.setCreatedAt(Instant.now());

        return new CustomerVoucherResponse(
                customerVoucherRepository.save(voucher)
        );
    }

    public CustomerVoucherResponse update(Integer id, CustomerVoucherRequest request) {

        CustomerVoucher voucher = customerVoucherRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("CustomerVoucher not found with id: " + id)
                );

        // FIX #3: REMOVED - Allow admin to deactivate voucher anytime
        // Admin có quyền vô hiệu hóa voucher bất cứ lúc nào (ví dụ: phát hiện lỗi, khách hàng khiếu nại)

        if (request.getExpiresAt() != null &&
                request.getIssuedAt() != null &&
                request.getExpiresAt().isBefore(request.getIssuedAt())) {
            throw new IllegalArgumentException("Ngày hết hạn phải sau ngày phát hành");
        }

        voucher.setIssuedAt(request.getIssuedAt());
        voucher.setExpiresAt(request.getExpiresAt());
        
        // FIX #4: Validate remaining_uses and auto-update status
        if (request.getRemainingQuantity() != null) {
            if (request.getRemainingQuantity() < 0) {
                throw new IllegalArgumentException("Số lượt sử dụng không được âm");
            }
            voucher.setRemainingQuantity(request.getRemainingQuantity());
            
            // Auto-update status when remaining_uses = 0
            if (request.getRemainingQuantity() == 0) {
                voucher.setVoucherStatus(VoucherStatus.DA_DUNG);
            }
        }

        // Allow admin to manually activate voucher (if not used up)
        if (!VoucherStatus.HOAT_DONG.equals(voucher.getVoucherStatus()) && Boolean.TRUE.equals(request.getIsActive())) {
            // Only allow activation if voucher still has remaining uses
            if (voucher.getRemainingQuantity() != null && voucher.getRemainingQuantity() > 0) {
                voucher.setVoucherStatus(VoucherStatus.HOAT_DONG);
            } else {
                throw new IllegalArgumentException("Không thể kích hoạt voucher đã hết lượt sử dụng");
            }
        }
        
        // Allow admin to manually deactivate voucher
        if (VoucherStatus.HOAT_DONG.equals(voucher.getVoucherStatus()) && Boolean.FALSE.equals(request.getIsActive())) {
            voucher.setVoucherStatus(VoucherStatus.KHONG_HOAT_DONG);
        }

        return new CustomerVoucherResponse(
                customerVoucherRepository.save(voucher)
        );
    }

    public void delete(Integer id) {

        CustomerVoucher voucher = customerVoucherRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("CustomerVoucher not found with id: " + id)
                );

        voucher.setVoucherStatus(VoucherStatus.KHONG_HOAT_DONG);
        customerVoucherRepository.save(voucher);
    }
}
