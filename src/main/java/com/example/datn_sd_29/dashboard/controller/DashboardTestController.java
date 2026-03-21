package com.example.datn_sd_29.dashboard.controller;

import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/test")
@RequiredArgsConstructor
public class DashboardTestController {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final CustomerRepository customerRepository;
    private final DiningTableRepository diningTableRepository;

    @GetMapping("/data-check")
    public Map<String, Object> checkData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check invoices
            Long totalInvoices = invoiceRepository.count();
            result.put("totalInvoices", totalInvoices);
            
            // Check PAID invoices
            Long paidInvoices = invoiceRepository.findAll().stream()
                .filter(i -> "PAID".equals(i.getInvoiceStatus()))
                .count();
            result.put("paidInvoices", paidInvoices);
            
            // Check today's invoices
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
            Long todayInvoices = invoiceRepository.countByDateRange(startOfDay, endOfDay);
            result.put("todayInvoices", todayInvoices);
            
            // Check invoice items
            Long totalInvoiceItems = invoiceItemRepository.count();
            result.put("totalInvoiceItems", totalInvoiceItems);
            
            // Check customers
            Long totalCustomers = customerRepository.count();
            result.put("totalCustomers", totalCustomers);
            
            // Check tables
            Long totalTables = diningTableRepository.count();
            Long occupiedTables = diningTableRepository.countByTableStatus("OCCUPIED");
            result.put("totalTables", totalTables);
            result.put("occupiedTables", occupiedTables);
            
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getName());
        }
        
        return result;
    }
    
    @GetMapping("/query-test")
    public Map<String, Object> testQueries() {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        try {
            // Test revenue query
            var revenue = invoiceRepository.sumFinalAmountByDateRange(startOfDay, endOfDay);
            result.put("todayRevenue", revenue);
            
            // Test top products query
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, 5);
            var topProducts = invoiceItemRepository.findTopProductsByDateRangeRaw(
                startOfDay, endOfDay, pageable);
            result.put("topProductsCount", topProducts.size());
            result.put("topProductsSample", topProducts.isEmpty() ? null : topProducts.get(0));
            
            // Test recent invoices query
            var recentInvoices = invoiceRepository.findRecentInvoicesRaw(pageable);
            result.put("recentInvoicesCount", recentInvoices.size());
            result.put("recentInvoicesSample", recentInvoices.isEmpty() ? null : recentInvoices.get(0));
            
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getName());
            result.put("stackTrace", e.getStackTrace()[0].toString());
        }
        
        return result;
    }
}
