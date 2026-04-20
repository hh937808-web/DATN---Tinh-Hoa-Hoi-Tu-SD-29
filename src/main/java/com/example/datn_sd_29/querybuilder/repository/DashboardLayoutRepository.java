package com.example.datn_sd_29.querybuilder.repository;

import com.example.datn_sd_29.querybuilder.entity.DashboardLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardLayoutRepository extends JpaRepository<DashboardLayout, Long> {
    @Query("SELECT d FROM DashboardLayout d WHERE d.employeeId = :employeeId ORDER BY d.yPosition ASC, d.xPosition ASC")
    List<DashboardLayout> findByEmployeeIdOrderByYPositionAscXPositionAsc(@Param("employeeId") Integer employeeId);
    
    @Query("SELECT d FROM DashboardLayout d WHERE d.dashboardId = :dashboardId ORDER BY d.yPosition ASC, d.xPosition ASC")
    List<DashboardLayout> findByDashboardIdOrderByYPositionAscXPositionAsc(@Param("dashboardId") Long dashboardId);
    
    @Query("SELECT d FROM DashboardLayout d WHERE d.dashboardId = :dashboardId AND d.employeeId = :employeeId ORDER BY d.yPosition ASC, d.xPosition ASC")
    List<DashboardLayout> findByDashboardIdAndEmployeeIdOrderByYPositionAscXPositionAsc(
        @Param("dashboardId") Long dashboardId, 
        @Param("employeeId") Integer employeeId
    );
    
    Optional<DashboardLayout> findByEmployeeIdAndSavedQueryId(Integer employeeId, Long savedQueryId);
    
    Optional<DashboardLayout> findByDashboardIdAndSavedQueryId(Long dashboardId, Long savedQueryId);
    
    Optional<DashboardLayout> findByDashboardIdAndEmployeeIdAndSavedQueryId(
        Long dashboardId, 
        Integer employeeId, 
        Long savedQueryId
    );
    
    void deleteByEmployeeIdAndSavedQueryId(Integer employeeId, Long savedQueryId);
    
    void deleteByDashboardIdAndSavedQueryId(Long dashboardId, Long savedQueryId);
    
    void deleteByDashboardIdAndEmployeeIdAndSavedQueryId(
        Long dashboardId, 
        Integer employeeId, 
        Long savedQueryId
    );
    
    void deleteByDashboardId(Long dashboardId);
}
