package com.example.datn_sd_29.querybuilder.service;

import com.example.datn_sd_29.querybuilder.dto.DashboardRequest;
import com.example.datn_sd_29.querybuilder.dto.DashboardResponse;
import com.example.datn_sd_29.querybuilder.entity.Dashboard;
import com.example.datn_sd_29.querybuilder.repository.DashboardRepository;
import com.example.datn_sd_29.querybuilder.repository.DashboardLayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomDashboardService {
    
    private final DashboardRepository dashboardRepository;
    private final DashboardLayoutRepository dashboardLayoutRepository;
    
    public List<DashboardResponse> getMyDashboards(Integer employeeId) {
        List<Dashboard> dashboards = dashboardRepository.findByEmployeeIdOrderByUpdatedAtDesc(employeeId);
        return dashboards.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public DashboardResponse createDashboard(Integer employeeId, DashboardRequest request) {
        Dashboard dashboard = new Dashboard();
        dashboard.setEmployeeId(employeeId);
        dashboard.setDashboardName(request.getDashboardName());
        dashboard.setDescription(request.getDescription());
        
        Dashboard saved = dashboardRepository.save(dashboard);
        return toResponse(saved);
    }
    
    @Transactional
    public DashboardResponse updateDashboard(Long dashboardId, Integer employeeId, DashboardRequest request) {
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new RuntimeException("Dashboard not found: " + dashboardId));
        
        // Check ownership
        if (!dashboard.getEmployeeId().equals(employeeId)) {
            throw new SecurityException("You don't have permission to update this dashboard");
        }
        
        dashboard.setDashboardName(request.getDashboardName());
        dashboard.setDescription(request.getDescription());
        
        Dashboard updated = dashboardRepository.save(dashboard);
        return toResponse(updated);
    }
    
    @Transactional
    public void deleteDashboard(Long dashboardId, Integer employeeId) {
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new RuntimeException("Dashboard not found: " + dashboardId));
        
        // Check ownership
        if (!dashboard.getEmployeeId().equals(employeeId)) {
            throw new SecurityException("You don't have permission to delete this dashboard");
        }
        
        // Delete all layouts associated with this dashboard (CASCADE will handle this)
        dashboardRepository.delete(dashboard);
    }
    
    public DashboardResponse getDashboard(Long dashboardId, Integer employeeId) {
        Dashboard dashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new RuntimeException("Dashboard not found: " + dashboardId));
        
        // Check ownership
        if (!dashboard.getEmployeeId().equals(employeeId)) {
            throw new SecurityException("You don't have permission to view this dashboard");
        }
        
        return toResponse(dashboard);
    }
    
    private DashboardResponse toResponse(Dashboard dashboard) {
        return new DashboardResponse(
                dashboard.getId(),
                dashboard.getDashboardName(),
                dashboard.getDescription(),
                dashboard.getCreatedAt(),
                dashboard.getUpdatedAt()
        );
    }
}
