package com.example.datn_sd_29.service;

import com.example.datn_sd_29.dto.CustomerDensityHourDTO;
import com.example.datn_sd_29.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceService {
    @Autowired
    private InvoiceRepository invoiceRepository;

    public List<CustomerDensityHourDTO> getCustomerDensityByHour() {
        return invoiceRepository.getCustomerDensityByHour()
                .stream()
                .map(obj -> {
                    CustomerDensityHourDTO dto = new CustomerDensityHourDTO();
                    dto.setHour(((Number) obj[0]).intValue());
                    dto.setTotalCustomers(((Number) obj[1]).longValue());
                    return dto;
                })
                .toList();
    }
}
