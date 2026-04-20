package com.example.datn_sd_29.querybuilder.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_queries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "query_definition", nullable = false, columnDefinition = "TEXT")
    private String queryDefinition; // JSON string of QueryRequest
    
    @Column(name = "visualization_type", nullable = false)
    private String visualizationType;
    
    @Column(name = "created_by", nullable = false)
    private Integer createdBy; // Employee ID
    
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
