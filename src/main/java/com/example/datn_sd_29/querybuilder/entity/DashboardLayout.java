package com.example.datn_sd_29.querybuilder.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_layouts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardLayout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;
    
    @Column(name = "dashboard_id", nullable = false)
    private Long dashboardId;
    
    @Column(name = "saved_query_id", nullable = false)
    private Long savedQueryId;
    
    @Column(name = "x_position", nullable = false)
    private Integer xPosition;
    
    @Column(name = "y_position", nullable = false)
    private Integer yPosition;
    
    @Column(nullable = false)
    private Integer width;
    
    @Column(nullable = false)
    private Integer height;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
