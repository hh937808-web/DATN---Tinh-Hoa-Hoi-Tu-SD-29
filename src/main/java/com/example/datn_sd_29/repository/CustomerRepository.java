package com.example.datn_sd_29.repository;

import com.example.datn_sd_29.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    boolean existsByEmailIgnoreCase(String email);
}
