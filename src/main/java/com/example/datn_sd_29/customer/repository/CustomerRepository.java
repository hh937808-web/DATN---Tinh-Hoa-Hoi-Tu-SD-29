package com.example.datn_sd_29.customer.repository;

import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.entity.Gender;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Long countByCreatedAtBetween(Instant start, Instant end);

    // Profile management methods (from dev)
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);

    // Search and filter methods (from hhieu)
    @Query("""
    SELECT c FROM Customer c
    WHERE (
        :keyword IS NULL OR
        CAST(c.id AS string) LIKE %:keyword% OR
        LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        c.phoneNumber LIKE %:keyword% OR
        LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
    AND (:isActive IS NULL OR c.isActive = :isActive)
    AND (:gender IS NULL OR c.gender = :gender)
    AND (:start IS NULL OR c.createdAt >= :start)
    AND (:end IS NULL OR c.createdAt < :end)
""")
    List<Customer> search(
            @Param("keyword") String keyword,
            @Param("isActive") Boolean isActive,
            @Param("gender") Gender gender,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
