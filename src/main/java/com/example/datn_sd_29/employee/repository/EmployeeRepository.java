package com.example.datn_sd_29.employee.repository;

import com.example.datn_sd_29.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    @Query("""
        SELECT e FROM Employee e
        WHERE (:username IS NULL OR e.username LIKE %:username%)
        AND (:role IS NULL OR e.role LIKE %:role%)
        AND (:phone IS NULL OR e.phoneNumber LIKE %:phone%)
        AND (:email IS NULL OR e.email LIKE %:email%)
        AND (:status IS NULL OR e.isActive = :status)
    """)
    List<Employee> searchEmployee(
            @Param("username") String username,
            @Param("role") String role,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("status") Boolean status
    );
}
