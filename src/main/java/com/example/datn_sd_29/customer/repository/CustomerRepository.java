package com.example.datn_sd_29.customer.repository;

import com.example.datn_sd_29.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Long countByCreatedAtBetween(Instant start, Instant end);

    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
}
