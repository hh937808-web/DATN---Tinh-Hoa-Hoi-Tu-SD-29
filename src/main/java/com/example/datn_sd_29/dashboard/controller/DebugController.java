package com.example.datn_sd_29.dashboard.controller;

import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DebugController {
    
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    
    @GetMapping("/invoice-tables/{invoiceId}")
    public Map<String, Object> getInvoiceTables(@PathVariable Integer invoiceId) {
        List<InvoiceDiningTable> tables = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoiceId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("invoiceId", invoiceId);
        result.put("tableCount", tables.size());
        result.put("tables", tables.stream()
            .map(idt -> {
                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("invoiceDiningTableId", idt.getId());
                tableInfo.put("diningTableId", idt.getDiningTable() != null ? idt.getDiningTable().getId() : null);
                tableInfo.put("tableName", idt.getDiningTable() != null ? idt.getDiningTable().getTableName() : null);
                return tableInfo;
            })
            .collect(Collectors.toList()));
        
        // Test sorting
        List<String> sortedByIdNames = tables.stream()
            .filter(idt -> idt.getDiningTable() != null)
            .sorted((a, b) -> a.getDiningTable().getId().compareTo(b.getDiningTable().getId()))
            .map(idt -> idt.getDiningTable().getTableName())
            .collect(Collectors.toList());
        
        result.put("sortedByIdNames", sortedByIdNames);
        result.put("joinedNames", String.join(" + ", sortedByIdNames));
        
        return result;
    }
}
