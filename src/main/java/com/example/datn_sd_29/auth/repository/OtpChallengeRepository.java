package com.example.datn_sd_29.auth.repository;

import com.example.datn_sd_29.auth.entity.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Integer> {

    @Query("""
            select o from OtpChallenge o
            where lower(o.email) = lower(:email)
                and o.purpose = :purpose
                and o.consumedAt is null
                and o.invalidatedAt is null
            order by o.createdAt desc
            """)
    Optional<OtpChallenge> findLatestActive(@Param("email") String email,
                                            @Param("purpose") String purpose);
}
