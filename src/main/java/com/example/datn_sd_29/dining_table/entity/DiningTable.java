package com.example.datn_sd_29.dining_table.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Getter
@Setter
@Entity
public class DiningTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dining_table_id", nullable = false)
    private Integer id;

    @Size(max = 20)
    @Nationalized
    @Column(name = "table_name", length = 20)
    private String tableName;

    @Column(name = "seating_capacity")
    private Integer seatingCapacity;

    @Size(max = 50)
    @Nationalized
    @Column(name = "table_status", length = 50)
    private String tableStatus;

    @ColumnDefault("getdate()")
    @Column(name = "created_at")
    private Instant createdAt;

}