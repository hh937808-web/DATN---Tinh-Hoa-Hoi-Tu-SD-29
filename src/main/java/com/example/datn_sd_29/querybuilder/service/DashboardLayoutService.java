package com.example.datn_sd_29.querybuilder.service;

import com.example.datn_sd_29.querybuilder.dto.DashboardLayoutRequest;
import com.example.datn_sd_29.querybuilder.dto.DashboardLayoutResponse;
import com.example.datn_sd_29.querybuilder.entity.DashboardLayout;
import com.example.datn_sd_29.querybuilder.repository.DashboardLayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardLayoutService {
    
    private final DashboardLayoutRepository dashboardLayoutRepository;
    
    // Get layouts for a specific dashboard
    public List<DashboardLayoutResponse> getDashboardLayouts(Long dashboardId, Integer employeeId) {
        List<DashboardLayout> layouts = dashboardLayoutRepository
                .findByDashboardIdAndEmployeeIdOrderByYPositionAscXPositionAsc(dashboardId, employeeId);
        
        return layouts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    // Save layouts for a specific dashboard
    @Transactional
    public void saveDashboardLayouts(Long dashboardId, Integer employeeId, List<DashboardLayoutRequest> requests) {
        for (DashboardLayoutRequest request : requests) {
            log.info("Saving layout for dashboard {} query {}: x={}, y={}, w={}, h={}", 
                dashboardId, request.getSavedQueryId(), request.getX(), request.getY(), request.getW(), request.getH());
            
            DashboardLayout layout = dashboardLayoutRepository
                    .findByDashboardIdAndEmployeeIdAndSavedQueryId(dashboardId, employeeId, request.getSavedQueryId())
                    .orElse(new DashboardLayout());
            
            layout.setDashboardId(dashboardId);
            layout.setEmployeeId(employeeId);
            layout.setSavedQueryId(request.getSavedQueryId());
            layout.setXPosition(request.getX());
            layout.setYPosition(request.getY());
            layout.setWidth(request.getW());
            layout.setHeight(request.getH());
            
            dashboardLayoutRepository.save(layout);
        }
    }
    
    // Remove layout from a specific dashboard
    @Transactional
    public void removeDashboardLayout(Long dashboardId, Integer employeeId, Long savedQueryId) {
        dashboardLayoutRepository.deleteByDashboardIdAndEmployeeIdAndSavedQueryId(dashboardId, employeeId, savedQueryId);
    }
    
    // Legacy methods for backward compatibility (without dashboard ID)
    public List<DashboardLayoutResponse> getMyLayouts(Integer employeeId) {
        List<DashboardLayout> layouts = dashboardLayoutRepository
                .findByEmployeeIdOrderByYPositionAscXPositionAsc(employeeId);
        
        return layouts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void saveLayouts(Integer employeeId, List<DashboardLayoutRequest> requests) {
        for (DashboardLayoutRequest request : requests) {
            log.info("Saving layout for query {}: x={}, y={}, w={}, h={}", 
                request.getSavedQueryId(), request.getX(), request.getY(), request.getW(), request.getH());
            
            DashboardLayout layout = dashboardLayoutRepository
                    .findByEmployeeIdAndSavedQueryId(employeeId, request.getSavedQueryId())
                    .orElse(new DashboardLayout());
            
            layout.setEmployeeId(employeeId);
            layout.setSavedQueryId(request.getSavedQueryId());
            layout.setXPosition(request.getX());
            layout.setYPosition(request.getY());
            layout.setWidth(request.getW());
            layout.setHeight(request.getH());
            
            dashboardLayoutRepository.save(layout);
        }
    }
    
    @Transactional
    public void removeLayout(Integer employeeId, Long savedQueryId) {
        dashboardLayoutRepository.deleteByEmployeeIdAndSavedQueryId(employeeId, savedQueryId);
    }
    
    private DashboardLayoutResponse toResponse(DashboardLayout layout) {
        DashboardLayoutResponse response = new DashboardLayoutResponse();
        response.setId(layout.getId());
        response.setSavedQueryId(layout.getSavedQueryId());
        response.setX(layout.getXPosition());
        response.setY(layout.getYPosition());
        response.setW(layout.getWidth());
        response.setH(layout.getHeight());
        response.setI("widget-" + layout.getSavedQueryId());
        
        log.info("Mapping layout: id={}, savedQueryId={}, x={}, y={}, w={}, h={}", 
            layout.getId(), layout.getSavedQueryId(), layout.getXPosition(), 
            layout.getYPosition(), layout.getWidth(), layout.getHeight());
        
        return response;
    }
}
