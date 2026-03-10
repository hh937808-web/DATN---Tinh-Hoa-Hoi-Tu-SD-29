package com.example.datn_sd_29.customer.controller;

import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping("/search")
    public List<Customer> searchCustomer(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean status
    ){
        return customerService.searchCustomer(phone,name,email,status);
    }

    @GetMapping("/sort")
    public List<Customer> sortCustomer(
            @RequestParam String field,
            @RequestParam(defaultValue = "asc") String direction
    ){
        return customerService.sortCustomer(field, direction);
    }
}
