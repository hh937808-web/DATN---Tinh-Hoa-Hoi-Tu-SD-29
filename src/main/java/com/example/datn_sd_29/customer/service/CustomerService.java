package com.example.datn_sd_29.customer.service;

import com.example.datn_sd_29.customer.dto.CustomerResponse;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.entity.Gender;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class CustomerService {
    @Autowired
    private CustomerRepository customerRepository;

    public List<CustomerResponse> getAllSorted(String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return customerRepository.findAll(sort)
                .stream()
                .map(CustomerResponse::new)
                .toList();
    }

    public List<CustomerResponse> search(String keyword, Boolean isActive, Gender gender,
                                         String startDate, String endDate) {

        // fix keyword rỗng
        if (keyword != null && keyword.trim().isEmpty()) {
            keyword = null;
        }

        Instant start = null;
        Instant end = null;

        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDate.parse(startDate)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
        }

        if (endDate != null && !endDate.isEmpty()) {
            end = LocalDate.parse(endDate)
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant();
        }

        return java.util.Optional
                .ofNullable(customerRepository.search(keyword, isActive, gender, start, end))
                .orElse(List.of())
                .stream()
                .filter(java.util.Objects::nonNull)
                .map(CustomerResponse::new)
                .toList();
    }
    public void updateStatus(Integer id, Boolean isActive) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setIsActive(isActive);
        customerRepository.save(customer);
    }
}