package com.example.datn_sd_29.repository;

import com.example.datn_sd_29.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Integer> {
}
