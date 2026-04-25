package com.example.datn_sd_29.dining_table.repository;

import com.example.datn_sd_29.dining_table.entity.DiningTable;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Integer> {

    List<DiningTable> findBySeatingCapacityGreaterThanEqual(Integer seatingCapacity);

    Long countByTableStatus(String tableStatus);

    // Đếm tổng bàn ngoại trừ 1 status (dùng để loại OUT_OF_SERVICE khỏi tổng bàn phục vụ)
    Long countByTableStatusNot(String excludedStatus);

    List<DiningTable> findByTableNameContainingIgnoreCaseAndTableStatusNot(
            String tableName,
            String excludedStatus
    );

    List<DiningTable> findByTableStatusAndTableStatusNot(
            String tableStatus,
            String excludedStatus
    );

    List<DiningTable> findBySeatingCapacityAndTableStatusNot(
            Integer seatingCapacity,
            String excludedStatus
    );

    List<DiningTable> findByTableStatusNot(
            String excludedStatus,
            Sort sort
    );

    @Query("""
        SELECT dt
        FROM DiningTable dt
        WHERE (:keyword IS NULL OR :keyword = '' 
               OR LOWER(dt.tableName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:seatingCapacity IS NULL OR dt.seatingCapacity = :seatingCapacity)
          AND (:tableStatus IS NULL OR :tableStatus = '' OR dt.tableStatus = :tableStatus)
          AND (:fromDate IS NULL OR dt.createdAt >= :fromDate)
          AND (:toDate IS NULL OR dt.createdAt <= :toDate)
    """)
    List<DiningTable> searchTables(
            @Param("keyword") String keyword,
            @Param("seatingCapacity") Integer seatingCapacity,
            @Param("tableStatus") String tableStatus,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Sort sort
    );
    // Batch table status update - Requirements 1.2, 9.1, 9.2
    @Modifying
    @Query("UPDATE DiningTable dt SET dt.tableStatus = :status WHERE dt.id IN :tableIds")
    void updateTableStatusByIdIn(@Param("tableIds") List<Integer> tableIds, 
                                  @Param("status") String status);

    // Pessimistic lock for reservation race condition prevention
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT dt FROM DiningTable dt WHERE dt.id IN :tableIds")
    List<DiningTable> lockTablesForReservation(@Param("tableIds") List<Integer> tableIds);

    // Find available tables by area and floor
    @Query("SELECT dt FROM DiningTable dt WHERE dt.area = :area AND dt.floor = :floor ORDER BY dt.id ASC")
    List<DiningTable> findByAreaAndFloorOrderById(@Param("area") String area, @Param("floor") Integer floor);

    // Find available tables by floor only
    @Query("SELECT dt FROM DiningTable dt WHERE dt.floor = :floor ORDER BY dt.area ASC, dt.id ASC")
    List<DiningTable> findByFloorOrderByAreaAndId(@Param("floor") Integer floor);
}
