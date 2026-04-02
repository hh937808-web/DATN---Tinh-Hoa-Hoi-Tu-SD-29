package com.example.datn_sd_29.employee.repository;

import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.enums.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    Optional<Employee> findByUsernameIgnoreCase(String username);
    Optional<Employee> findByEmailIgnoreCase(String email);
    Optional<Employee> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("""
    SELECT e FROM Employee e
    WHERE e.role <> 'ADMIN'
      AND (:keyword = '' OR
           LOWER(e.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(e.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           e.phoneNumber LIKE CONCAT('%', :keyword, '%')
      )
      AND (:role = '' OR e.role = :role)
      AND (:gender IS NULL OR e.gender = :gender)
      AND (:fromDate IS NULL OR e.createdAt >= :fromDate)
      AND (:toDate IS NULL OR e.createdAt <= :toDate)
      AND (:status IS NULL OR e.isActive = :status)  
    """)
    List<Employee> searchEmployee(
            @Param("keyword") String keyword,
            @Param("role") String role,
            @Param("gender") Gender gender,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("status") Boolean status   // 👈 THÊM PARAM
    );
}