package com.example.datn_sd_29.report.service;

import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
import com.example.datn_sd_29.product_combo.entity.ProductComboItem;
import com.example.datn_sd_29.product_combo.repository.ProductComboItemRepository;
import com.example.datn_sd_29.report.dto.ProductReportResponse;
import com.example.datn_sd_29.report.dto.RevenueReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductComboItemRepository productComboItemRepository;

    public RevenueReportResponse generateRevenueReport(LocalDate startDate, LocalDate endDate) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfPeriod = startDate.atStartOfDay(zoneId).toInstant();
        Instant endOfPeriod = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();
        
        // Get all paid invoices in the period
        List<Invoice> paidInvoices = invoiceRepository.findAll().stream()
            .filter(i -> "PAID".equals(i.getInvoiceStatus()))
            .filter(i -> i.getPaidAt() != null)
            .filter(i -> !i.getPaidAt().isBefore(startOfPeriod) && i.getPaidAt().isBefore(endOfPeriod))
            .collect(Collectors.toList());
        
        // Calculate totals
        BigDecimal totalRevenue = paidInvoices.stream()
            .map(Invoice::getFinalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int totalInvoices = paidInvoices.size();
        
        BigDecimal averageOrderValue = totalInvoices > 0 
            ? totalRevenue.divide(BigDecimal.valueOf(totalInvoices), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        // Calculate payment method breakdown
        Map<String, PaymentMethodData> paymentMethodMap = new HashMap<>();
        for (Invoice invoice : paidInvoices) {
            String method = invoice.getPaymentMethod() != null ? invoice.getPaymentMethod() : "UNKNOWN";
            PaymentMethodData data = paymentMethodMap.getOrDefault(method, new PaymentMethodData());
            data.amount = data.amount.add(invoice.getFinalAmount());
            data.count++;
            paymentMethodMap.put(method, data);
        }
        
        List<RevenueReportResponse.PaymentMethodBreakdown> paymentMethods = paymentMethodMap.entrySet().stream()
            .map(entry -> {
                double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
                return new RevenueReportResponse.PaymentMethodBreakdown(
                    entry.getKey(),
                    entry.getValue().amount,
                    entry.getValue().count,
                    percentage
                );
            })
            .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
            .collect(Collectors.toList());
        
        BigDecimal cashRevenue = paymentMethodMap.getOrDefault("CASH", new PaymentMethodData()).amount;
        BigDecimal transferRevenue = paymentMethodMap.getOrDefault("TRANSFER", new PaymentMethodData()).amount;
        
        // Calculate daily revenues
        List<RevenueReportResponse.DailyRevenue> dailyRevenues = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            final LocalDate date = currentDate;
            Instant dayStart = date.atStartOfDay(zoneId).toInstant();
            Instant dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant();
            
            BigDecimal dayRevenue = paidInvoices.stream()
                .filter(i -> !i.getPaidAt().isBefore(dayStart) && i.getPaidAt().isBefore(dayEnd))
                .map(Invoice::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            int dayInvoiceCount = (int) paidInvoices.stream()
                .filter(i -> !i.getPaidAt().isBefore(dayStart) && i.getPaidAt().isBefore(dayEnd))
                .count();
            
            dailyRevenues.add(new RevenueReportResponse.DailyRevenue(date, dayRevenue, dayInvoiceCount));
            currentDate = currentDate.plusDays(1);
        }
        
        // Calculate previous period comparison
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate previousStartDate = startDate.minusDays(periodDays);
        LocalDate previousEndDate = startDate.minusDays(1);
        
        Instant previousStartOfPeriod = previousStartDate.atStartOfDay(zoneId).toInstant();
        Instant previousEndOfPeriod = previousEndDate.plusDays(1).atStartOfDay(zoneId).toInstant();
        
        List<Invoice> previousPaidInvoices = invoiceRepository.findAll().stream()
            .filter(i -> "PAID".equals(i.getInvoiceStatus()))
            .filter(i -> i.getPaidAt() != null)
            .filter(i -> !i.getPaidAt().isBefore(previousStartOfPeriod) && i.getPaidAt().isBefore(previousEndOfPeriod))
            .collect(Collectors.toList());
        
        BigDecimal previousPeriodRevenue = previousPaidInvoices.stream()
            .map(Invoice::getFinalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int previousPeriodInvoices = previousPaidInvoices.size();
        
        // Calculate growth percentages
        Double revenueGrowthPercentage = null;
        if (previousPeriodRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal growth = totalRevenue.subtract(previousPeriodRevenue)
                .divide(previousPeriodRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            revenueGrowthPercentage = growth.doubleValue();
        }
        
        Double invoiceGrowthPercentage = null;
        if (previousPeriodInvoices > 0) {
            invoiceGrowthPercentage = ((double) (totalInvoices - previousPeriodInvoices) / previousPeriodInvoices) * 100;
        }
        
        // Calculate hourly revenues (peak hours)
        Map<Integer, HourlyData> hourlyMap = new HashMap<>();
        for (Invoice invoice : paidInvoices) {
            int hour = invoice.getPaidAt().atZone(zoneId).getHour();
            HourlyData data = hourlyMap.getOrDefault(hour, new HourlyData());
            data.revenue = data.revenue.add(invoice.getFinalAmount());
            data.count++;
            hourlyMap.put(hour, data);
        }
        
        List<RevenueReportResponse.HourlyRevenue> hourlyRevenues = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            HourlyData data = hourlyMap.getOrDefault(hour, new HourlyData());
            hourlyRevenues.add(new RevenueReportResponse.HourlyRevenue(hour, data.revenue, data.count));
        }
        
        // Calculate category breakdown
        Map<String, CategoryData> categoryMap = new HashMap<>();
        List<InvoiceItem> allItems = invoiceItemRepository.findAll();
        
        for (InvoiceItem item : allItems) {
            if (item.getInvoice() == null) continue;
            if (!"PAID".equals(item.getInvoice().getInvoiceStatus())) continue;
            if (item.getInvoice().getPaidAt() == null) continue;
            if (item.getInvoice().getPaidAt().isBefore(startOfPeriod) || 
                !item.getInvoice().getPaidAt().isBefore(endOfPeriod)) continue;
            
            // Handle single product items
            if (item.getProduct() != null && "PRODUCT".equals(item.getItemType())) {
                String category = item.getProduct().getProductCategory() != null ? 
                    item.getProduct().getProductCategory().toString() : "UNKNOWN";
                CategoryData data = categoryMap.getOrDefault(category, new CategoryData());
                data.revenue = data.revenue.add(
                    item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                );
                data.itemCount += item.getQuantity();
                categoryMap.put(category, data);
            }
            
            // Handle combo items - expand to individual products
            if (item.getProductCombo() != null && "COMBO".equals(item.getItemType())) {
                List<ProductComboItem> comboItems = productComboItemRepository
                    .findByProductComboId(item.getProductCombo().getId());
                
                // Calculate price per combo item proportionally
                BigDecimal comboTotalPrice = item.getUnitPrice();
                
                for (ProductComboItem comboItem : comboItems) {
                    if (comboItem.getProduct() == null) continue;
                    
                    String category = comboItem.getProduct().getProductCategory() != null ? 
                        comboItem.getProduct().getProductCategory().toString() : "UNKNOWN";
                    CategoryData data = categoryMap.getOrDefault(category, new CategoryData());
                    
                    // Calculate proportional revenue for this product in combo
                    // Use product's price to calculate proportion
                    BigDecimal productPrice = comboItem.getProduct().getUnitPrice();
                    BigDecimal productRevenue = productPrice.multiply(BigDecimal.valueOf(comboItem.getQuantity()))
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                    
                    data.revenue = data.revenue.add(productRevenue);
                    data.itemCount += (comboItem.getQuantity() * item.getQuantity());
                    categoryMap.put(category, data);
                }
            }
        }
        
        List<RevenueReportResponse.CategoryBreakdown> categoryBreakdown = categoryMap.entrySet().stream()
            .map(entry -> {
                double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().revenue.multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
                return new RevenueReportResponse.CategoryBreakdown(
                    entry.getKey(),
                    entry.getValue().revenue,
                    entry.getValue().itemCount,
                    percentage
                );
            })
            .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
            .collect(Collectors.toList());
        
        RevenueReportResponse response = new RevenueReportResponse(
            startDate,
            endDate,
            totalRevenue,
            totalInvoices,
            averageOrderValue,
            cashRevenue,
            transferRevenue,
            dailyRevenues,
            paymentMethods,
            previousPeriodRevenue,
            revenueGrowthPercentage,
            previousPeriodInvoices,
            invoiceGrowthPercentage,
            hourlyRevenues,
            categoryBreakdown
        );
        
        return response;
    }

    public ProductReportResponse generateProductReport(LocalDate startDate, LocalDate endDate, int limit) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfPeriod = startDate.atStartOfDay(zoneId).toInstant();
        Instant endOfPeriod = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();
        
        // Get all invoice items from paid invoices in the period
        List<InvoiceItem> allItems = invoiceItemRepository.findAll();
        
        // Group by product and calculate totals
        Map<Integer, ProductData> productMap = new HashMap<>();
        
        for (InvoiceItem item : allItems) {
            if (item.getInvoice() == null) continue;
            if (!"PAID".equals(item.getInvoice().getInvoiceStatus())) continue;
            if (item.getInvoice().getPaidAt() == null) continue;
            if (item.getInvoice().getPaidAt().isBefore(startOfPeriod) || 
                !item.getInvoice().getPaidAt().isBefore(endOfPeriod)) continue;
            
            // Handle single product items
            if (item.getProduct() != null && "PRODUCT".equals(item.getItemType())) {
                Integer productId = item.getProduct().getId();
                ProductData data = productMap.getOrDefault(productId, 
                    new ProductData(
                        productId,
                        item.getProduct().getProductName(),
                        item.getProduct().getProductCategory() != null ? 
                            item.getProduct().getProductCategory().toString() : "UNKNOWN"
                    ));
                
                data.quantity += item.getQuantity();
                data.revenue = data.revenue.add(
                    item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                );
                
                productMap.put(productId, data);
            }
            
            // Handle combo items - expand to individual products
            if (item.getProductCombo() != null && "COMBO".equals(item.getItemType())) {
                List<ProductComboItem> comboItems = productComboItemRepository
                    .findByProductComboId(item.getProductCombo().getId());
                
                for (ProductComboItem comboItem : comboItems) {
                    if (comboItem.getProduct() == null) continue;
                    
                    Integer productId = comboItem.getProduct().getId();
                    ProductData data = productMap.getOrDefault(productId, 
                        new ProductData(
                            productId,
                            comboItem.getProduct().getProductName(),
                            comboItem.getProduct().getProductCategory() != null ? 
                                comboItem.getProduct().getProductCategory().toString() : "UNKNOWN"
                        ));
                    
                    // Add quantity from combo (combo quantity * product quantity in combo)
                    int totalQuantity = comboItem.getQuantity() * item.getQuantity();
                    data.quantity += totalQuantity;
                    
                    // Calculate revenue proportionally based on product price
                    BigDecimal productRevenue = comboItem.getProduct().getUnitPrice()
                        .multiply(BigDecimal.valueOf(totalQuantity));
                    data.revenue = data.revenue.add(productRevenue);
                    
                    productMap.put(productId, data);
                }
            }
        }
        
        // Sort by revenue
        List<ProductData> sortedByRevenue = productMap.values().stream()
            .sorted((a, b) -> b.revenue.compareTo(a.revenue))
            .limit(limit)
            .collect(Collectors.toList());
        
        // Sort by quantity
        List<ProductData> sortedByQuantity = productMap.values().stream()
            .sorted((a, b) -> Integer.compare(b.quantity, a.quantity))
            .limit(limit)
            .collect(Collectors.toList());
        
        // Calculate percentages for revenue
        BigDecimal maxRevenue = sortedByRevenue.isEmpty() ? BigDecimal.ZERO : sortedByRevenue.get(0).revenue;
        List<ProductReportResponse.ProductSales> topByRevenue = sortedByRevenue.stream()
            .map(data -> {
                int percentage = maxRevenue.compareTo(BigDecimal.ZERO) > 0 
                    ? data.revenue.multiply(BigDecimal.valueOf(100))
                        .divide(maxRevenue, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
                return new ProductReportResponse.ProductSales(
                    data.id, data.name, data.category, data.quantity, data.revenue, percentage
                );
            })
            .collect(Collectors.toList());
        
        // Calculate percentages for quantity
        int maxQuantity = sortedByQuantity.isEmpty() ? 0 : sortedByQuantity.get(0).quantity;
        List<ProductReportResponse.ProductSales> topByQuantity = sortedByQuantity.stream()
            .map(data -> {
                int percentage = maxQuantity > 0 ? (data.quantity * 100 / maxQuantity) : 0;
                return new ProductReportResponse.ProductSales(
                    data.id, data.name, data.category, data.quantity, data.revenue, percentage
                );
            })
            .collect(Collectors.toList());
        
        return new ProductReportResponse(startDate, endDate, topByRevenue, topByQuantity);
    }

    // Helper classes
    private static class PaymentMethodData {
        BigDecimal amount = BigDecimal.ZERO;
        int count = 0;
    }
    
    private static class HourlyData {
        BigDecimal revenue = BigDecimal.ZERO;
        int count = 0;
    }
    
    private static class CategoryData {
        BigDecimal revenue = BigDecimal.ZERO;
        int itemCount = 0;
    }

    private static class ProductData {
        Integer id;
        String name;
        String category;
        int quantity = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        
        ProductData(Integer id, String name, String category) {
            this.id = id;
            this.name = name;
            this.category = category;
        }
    }
}
