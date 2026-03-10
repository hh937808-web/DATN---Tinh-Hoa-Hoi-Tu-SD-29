package com.example.datn_sd_29.dining_table.repository;

import com.example.datn_sd_29.dining_table.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Integer> {
    List<DiningTable> findBySeatingCapacityGreaterThanEqual(Integer seatingCapacity);
}
