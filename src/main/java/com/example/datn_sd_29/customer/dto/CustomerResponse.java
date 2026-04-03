package com.example.datn_sd_29.customer.dto;

import com.example.datn_sd_29.customer.entity.Customer;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    //Sort
    private Integer id;
    private Integer loyaltyPoints;
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Instant createdAt;

    //Search
    private String fullName;
    private String phoneNumber;
    private String email;
    private Boolean isActive;
    private String gender;
    private LocalDate dateOfBirth;

    // Constructor map từ Entity
    public CustomerResponse(Customer customer) {
        if (customer != null) {
            this.id = customer.getId();
            this.fullName = customer.getFullName();
            this.phoneNumber = customer.getPhoneNumber();
            this.email = customer.getEmail();
            this.loyaltyPoints = customer.getLoyaltyPoints();
            this.isActive = customer.getIsActive();
            this.createdAt = customer.getCreatedAt();
            this.dateOfBirth = customer.getDateOfBirth();
            if (customer.getGender() != null) {
                switch (customer.getGender()) {
                    case MALE -> this.gender = "Nam";
                    case FEMALE -> this.gender = "Nữ";
                    case OTHER -> this.gender = "Khác";
                }
            } else {
                this.gender = "Khác"; // fallback
            }
        }
    }
}