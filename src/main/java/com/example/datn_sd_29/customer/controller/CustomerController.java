package com.example.datn_sd_29.customer.controller;

import com.example.datn_sd_29.customer.dto.CustomerResponse;
import com.example.datn_sd_29.customer.entity.Gender;
import com.example.datn_sd_29.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin("*")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // GET ALL
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAll() {
        return ResponseEntity.ok(customerService.getAllSorted("id", "asc"));
    }

    // SEARCH
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Gender gender,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return ResponseEntity.ok(
                customerService.search(keyword, isActive, gender, startDate, endDate)
        );
    }

    // SORT
    @GetMapping("/sort")
    public ResponseEntity<List<CustomerResponse>> sort(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return ResponseEntity.ok(customerService.getAllSorted(sortBy, direction));
    }

    // UPDATE STATUS
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean isActive = body.get("isActive");
        customerService.updateStatus(id, isActive);
        return ResponseEntity.ok().build();
    }
}