package com.example.datn_sd_29.invoice.entity;

import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.employee.entity.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Column(name = "invoice_code", length = 100)
    private String invoiceCode;

    @Size(max = 100)
    @Column(name = "reservation_code", length = 100)
    private String reservationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Size(max = 20)
    @Column(name = "invoice_channel", length = 20)
    private String invoiceChannel;

    @Size(max = 50)
    @Column(name = "invoice_status", length = 50)
    private String invoiceStatus;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "subtotal_amount", precision = 18, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @ColumnDefault("isnull([subtotal_amount], 0)-isnull([discount_amount], 0)")
    @Column(name = "final_amount",insertable = false,updatable = false, precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @Size(max = 20)
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "earned_points")
    private Integer earnedPoints;

    @Column(name = "used_points")
    private Integer usedPoints;

}