package com.example.datn_sd_29.controller;

import com.example.datn_sd_29.dto.CustomerDensityHourDTO;
import com.example.datn_sd_29.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticController {
    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/customer-density/hour")
    public List<CustomerDensityHourDTO> customerDensityByHour() {
        return invoiceService.getCustomerDensityByHour();
    }
}
