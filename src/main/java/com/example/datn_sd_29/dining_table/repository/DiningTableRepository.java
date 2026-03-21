package com.example.datn_sd_29.dining_table.repository;

import com.example.datn_sd_29.dining_table.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Integer> {
    List<DiningTable> findBySeatingCapacityGreaterThanEqual(Integer seatingCapacity);
    Long countByTableStatus(String tableStatus);

    // Batch table status update - Requirements 1.2, 9.1, 9.2
    @Modifying
    @Query("UPDATE DiningTable dt SET dt.tableStatus = :status WHERE dt.id IN :tableIds")
    void updateTableStatusByIdIn(@Param("tableIds") List<Integer> tableIds, 
                                  @Param("status") String status);
}
