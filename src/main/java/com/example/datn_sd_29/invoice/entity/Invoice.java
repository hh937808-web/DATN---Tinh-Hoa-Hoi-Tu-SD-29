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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serving_staff_id")
    private Employee servingStaff;

    @Size(max = 20)
    @Column(name = "invoice_channel", length = 20)
    private String invoiceChannel;

    @Size(max = 50)
    @Column(name = "invoice_status", length = 50)
    private String invoiceStatus;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Size(max = 200)
    @Column(name = "promotion_type", length = 200)
    private String promotionType;

    @Size(max = 500)
    @Column(name = "reservation_note", length = 500)
    private String reservationNote;

    @Size(max = 1000)
    @Column(name = "food_note", length = 1000)
    private String foodNote;

    @Size(max = 200)
    @Column(name = "guest_name", length = 200)
    private String guestName;

    @Size(max = 20)
    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "subtotal_amount", precision = 18, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "manual_discount_percent", precision = 5, scale = 2)
    private BigDecimal manualDiscountPercent;

    @Column(name = "manual_discount_amount", precision = 18, scale = 2)
    private BigDecimal manualDiscountAmount;

    @Column(name = "tax_percent", precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "service_fee_percent", precision = 5, scale = 2)
    private BigDecimal serviceFeePercent;

    @Column(name = "service_fee_amount", precision = 18, scale = 2)
    private BigDecimal serviceFeeAmount;

    @ColumnDefault("isnull([subtotal_amount], 0)-isnull([discount_amount], 0)+isnull([tax_amount], 0)+isnull([service_fee_amount], 0)")
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
