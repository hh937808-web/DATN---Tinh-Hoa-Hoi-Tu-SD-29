package com.example.datn_sd_29.voucher.service;

import com.example.datn_sd_29.voucher.dto.CustomerVoucherRequest;
import com.example.datn_sd_29.voucher.dto.CustomerVoucherResponse;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.voucher.entity.CustomerVoucher;
import com.example.datn_sd_29.voucher.entity.PersonalVoucher;
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

    public List<CustomerVoucherResponse> getAll() {
        return customerVoucherRepository.findAll()
                .stream()
                .map(CustomerVoucherResponse::new)
                .toList();
    }

    public CustomerVoucherResponse create(CustomerVoucherRequest request) {

        PersonalVoucher personalVoucher = personalVoucherRepository
                .findById(request.getPersonalVoucherId())
                .orElseThrow(() ->
                        new IllegalArgumentException("PersonalVoucher not found with id: " + request.getPersonalVoucherId())
                );

        Customer customer = customerRepository
                .findById(request.getCustomerId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer not found with id: " + request.getCustomerId())
                );

        if (request.getExpiresAt() != null &&
                request.getIssuedAt() != null &&
                request.getExpiresAt().isBefore(request.getIssuedAt())) {
            throw new IllegalArgumentException("Expire date must be after issued date");
        }

        CustomerVoucher voucher = new CustomerVoucher();
        voucher.setPersonalVoucher(personalVoucher);
        voucher.setCustomer(customer);
        voucher.setIssuedAt(request.getIssuedAt());
        voucher.setExpiresAt(request.getExpiresAt());
        voucher.setRemainingQuantity(request.getRemainingQuantity());
        voucher.setIsActive(true);
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

        if (voucher.getIsActive() && Boolean.FALSE.equals(request.getIsActive())) {
            throw new IllegalArgumentException("Cannot change status from ACTIVE to INACTIVE");
        }

        if (request.getExpiresAt() != null &&
                request.getIssuedAt() != null &&
                request.getExpiresAt().isBefore(request.getIssuedAt())) {
            throw new IllegalArgumentException("Expire date must be after issued date");
        }

        voucher.setIssuedAt(request.getIssuedAt());
        voucher.setExpiresAt(request.getExpiresAt());
        voucher.setRemainingQuantity(request.getRemainingQuantity());

        if (!voucher.getIsActive() && Boolean.TRUE.equals(request.getIsActive())) {
            voucher.setIsActive(true);
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

        voucher.setIsActive(false);
        customerVoucherRepository.save(voucher);
    }
}
