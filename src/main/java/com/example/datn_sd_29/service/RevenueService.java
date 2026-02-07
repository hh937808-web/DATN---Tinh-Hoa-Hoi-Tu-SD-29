package com.example.datn_sd_29.service;

import com.example.datn_sd_29.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RevenueService {
    private final InvoiceRepository invoiceRepository;

    public RevenueService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<Object[]> revenueByMonth() {
        return invoiceRepository.getRevenueByMonth();
    }

    public List<Object[]> revenueByYear() {
        return invoiceRepository.getRevenueByYear();
    }

    public List<Object[]> revenueByInvoice() {
        return invoiceRepository.getRevenueByInvoice();
    }
}
