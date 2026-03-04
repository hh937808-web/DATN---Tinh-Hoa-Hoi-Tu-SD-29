package com.example.datn_sd_29.auth.repository;

import com.example.datn_sd_29.auth.entity.PendingRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PendingRegisterRepository extends JpaRepository<PendingRegister, Integer> {

    @Query("""
           select p
           from PendingRegister p
           where lower(p.email) = lower(:email)
             and p.completedAt is null
             and p.invalidatedAt is null
           order by p.createdAt desc
           """)
    Optional<PendingRegister> findLatestActive(@Param("email") String email);
}
