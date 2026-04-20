package com.example.datn_sd_29.querybuilder.repository;

import com.example.datn_sd_29.querybuilder.entity.Dashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Long> {
    List<Dashboard> findByEmployeeIdOrderByUpdatedAtDesc(Integer employeeId);
}
