package com.example.datn_sd_29.employee.repository;

import com.example.datn_sd_29.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    Optional<Employee> findByUsernameIgnoreCase(String username);
    Optional<Employee> findByEmailIgnoreCase(String email);
}
