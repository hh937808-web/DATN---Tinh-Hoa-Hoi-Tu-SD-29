package com.example.datn_sd_29.dashboard.service;

import com.example.datn_sd_29.dashboard.dto.DashboardStatsResponse;
import com.example.datn_sd_29.dashboard.dto.RecentInvoiceResponse;
import com.example.datn_sd_29.dashboard.dto.TopProductResponse;
import com.example.datn_sd_29.dashboard.dto.InvoicePageResponse;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
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
public class DashboardService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final CustomerRepository customerRepository;
    private final DiningTableRepository diningTableRepository;

    public DashboardStatsResponse getStats(LocalDate startDate, LocalDate endDate) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfPeriod = startDate.atStartOfDay(zoneId).toInstant();
        Instant endOfPeriod = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();
        
        // Calculate previous period for comparison
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate prevStartDate = startDate.minusDays(daysDiff);
        LocalDate prevEndDate = startDate.minusDays(1);
        Instant startOfPrevPeriod = prevStartDate.atStartOfDay(zoneId).toInstant();
        Instant endOfPrevPeriod = prevEndDate.plusDays(1).atStartOfDay(zoneId).toInstant();

        // Get all invoices and filter in Java
        List<Invoice> allInvoices = invoiceRepository.findAll();
        
        // Current period revenue
        BigDecimal currentRevenue = allInvoices.stream()
            .filter(i -> "PAID".equals(i.getInvoiceStatus()))
            .filter(i -> i.getPaidAt() != null)
            .filter(i -> !i.getPaidAt().isBefore(startOfPeriod) && i.getPaidAt().isBefore(endOfPeriod))
            .map(Invoice::getFinalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal prevRevenue = allInvoices.stream()
            .filter(i -> "PAID".equals(i.getInvoiceStatus()))
            .filter(i -> i.getPaidAt() != null)
            .filter(i -> !i.getPaidAt().isBefore(startOfPrevPeriod) && i.getPaidAt().isBefore(endOfPrevPeriod))
            .map(Invoice::getFinalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Current period invoices
        long currentInvoiceCount = allInvoices.stream()
            .filter(i -> "PAID".equals(i.getInvoiceStatus()))
            .filter(i -> i.getPaidAt() != null)
            .filter(i -> !i.getPaidAt().isBefore(startOfPeriod) && i.getPaidAt().isBefore(endOfPeriod))
            .count();
            
        long prevInvoiceCount = allInvoices.stream()
            .filter(i -> "PAID".equals(i.getInvoiceStatus()))
            .filter(i -> i.getPaidAt() != null)
            .filter(i -> !i.getPaidAt().isBefore(startOfPrevPeriod) && i.getPaidAt().isBefore(endOfPrevPeriod))
            .count();
        
        // Customers - just count all for now
        Long currentCustomerCount = customerRepository.count();
        Long prevCustomerCount = 0L;
        
        // Active tables - count unique tables from IN_PROGRESS invoices
        Set<Integer> occupiedTableIds = new HashSet<>();
        allInvoices.stream()
            .filter(i -> "IN_PROGRESS".equals(i.getInvoiceStatus()))
            .filter(i -> i.getCheckedInAt() != null)
            .filter(i -> !i.getCheckedInAt().isAfter(Instant.now()))
            .filter(i -> {
                long minutesSinceCheckIn = java.time.temporal.ChronoUnit.MINUTES.between(
                    i.getCheckedInAt(), Instant.now()
                );
                return minutesSinceCheckIn <= 90;
            })
            .forEach(invoice -> {
                // Get all tables for this invoice
                List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
                invoiceTables.forEach(idt -> {
                    if (idt.getDiningTable() != null) {
                        occupiedTableIds.add(idt.getDiningTable().getId());
                    }
                });
            });
        
        Long occupiedTables = (long) occupiedTableIds.size();
        Long totalTables = diningTableRepository.count();

        DashboardStatsResponse response = new DashboardStatsResponse();
        
        response.setTodayRevenue(new DashboardStatsResponse.RevenueStats(
            currentRevenue,
            calculatePercentChange(currentRevenue, prevRevenue),
            getTrend(currentRevenue, prevRevenue)
        ));
        
        response.setTodayInvoices(new DashboardStatsResponse.InvoiceStats(
            (int) currentInvoiceCount,
            calculatePercentChange(currentInvoiceCount, prevInvoiceCount),
            getTrend(currentInvoiceCount, prevInvoiceCount)
        ));
        
        response.setTodayCustomers(new DashboardStatsResponse.CustomerStats(
            currentCustomerCount != null ? currentCustomerCount.intValue() : 0,
            calculatePercentChange(currentCustomerCount, prevCustomerCount),
            getTrend(currentCustomerCount, prevCustomerCount)
        ));
        
        int occupied = occupiedTables != null ? occupiedTables.intValue() : 0;
        int total = totalTables != null ? totalTables.intValue() : 0;
        int percentOccupied = total > 0 ? (occupied * 100 / total) : 0;
        
        response.setActiveTables(new DashboardStatsResponse.TableStats(
            occupied,
            total,
            percentOccupied
        ));
        
        return response;
    }

    public List<TopProductResponse> getTopProducts(LocalDate startDate, LocalDate endDate, int limit) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfPeriod = startDate.atStartOfDay(zoneId).toInstant();
        Instant endOfPeriod = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();
        
        // Get all invoice items and process in Java
        List<InvoiceItem> allItems = invoiceItemRepository.findAll();
        
        // Group by product and calculate totals
        Map<Integer, TopProductData> productMap = new HashMap<>();
        
        for (InvoiceItem item : allItems) {
            if (item.getInvoice() == null || item.getProduct() == null) continue;
            if (!"PAID".equals(item.getInvoice().getInvoiceStatus())) continue;
            if (item.getInvoice().getPaidAt() == null) continue;
            if (item.getInvoice().getPaidAt().isBefore(startOfPeriod) || 
                !item.getInvoice().getPaidAt().isBefore(endOfPeriod)) continue;
            
            Integer productId = item.getProduct().getId();
            TopProductData data = productMap.getOrDefault(productId, 
                new TopProductData(
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
        
        // Sort by revenue and take top N
        List<TopProductData> sorted = productMap.values().stream()
            .sorted((a, b) -> b.revenue.compareTo(a.revenue))
            .limit(limit)
            .collect(Collectors.toList());
        
        if (sorted.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calculate percentages
        BigDecimal maxRevenue = sorted.get(0).revenue;
        
        return sorted.stream()
            .map(data -> {
                int percentage = maxRevenue.compareTo(BigDecimal.ZERO) > 0 
                    ? data.revenue.multiply(BigDecimal.valueOf(100))
                        .divide(maxRevenue, 0, RoundingMode.HALF_UP)
                        .intValue()
                    : 0;
                
                return new TopProductResponse(
                    data.id,
                    data.name,
                    data.category,
                    data.quantity,
                    data.revenue,
                    percentage
                );
            })
            .collect(Collectors.toList());
    }

    public List<RecentInvoiceResponse> getRecentInvoices(int limit) {
        // Get all invoices and filter/sort in Java
        List<Invoice> allInvoices = invoiceRepository.findAll();
        Instant now = Instant.now();
        
        return allInvoices.stream()
            .filter(i -> {
                // Only show PAID and IN_PROGRESS invoices
                if (!"PAID".equals(i.getInvoiceStatus()) && 
                    !"IN_PROGRESS".equals(i.getInvoiceStatus())) {
                    return false;
                }
                
                // For PAID invoices, check paidAt is in the past
                if ("PAID".equals(i.getInvoiceStatus())) {
                    return i.getPaidAt() != null && !i.getPaidAt().isAfter(now);
                }
                
                // For IN_PROGRESS invoices, check checkedInAt is in the past
                if ("IN_PROGRESS".equals(i.getInvoiceStatus())) {
                    return i.getCheckedInAt() != null && !i.getCheckedInAt().isAfter(now);
                }
                
                return false;
            })
            .sorted((a, b) -> {
                Instant timeA = a.getPaidAt() != null ? a.getPaidAt() : a.getCheckedInAt();
                Instant timeB = b.getPaidAt() != null ? b.getPaidAt() : b.getCheckedInAt();
                if (timeA == null && timeB == null) return 0;
                if (timeA == null) return 1;
                if (timeB == null) return -1;
                return timeB.compareTo(timeA);
            })
            .limit(limit)
            .map(invoice -> {
                String tableName = "N/A";
                List<InvoiceDiningTable> tables = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
                if (tables != null && !tables.isEmpty()) {
                    // Show all combined tables (e.g., "Bàn 6 + Bàn 7")
                    // Sort by table ID to ensure consistent order
                    tableName = tables.stream()
                        .filter(idt -> idt.getDiningTable() != null)
                        .sorted((a, b) -> a.getDiningTable().getId().compareTo(b.getDiningTable().getId()))
                        .map(idt -> idt.getDiningTable().getTableName())
                        .collect(Collectors.joining(" + "));
                    
                    if (tableName.isEmpty()) {
                        tableName = "N/A";
                    }
                }
                
                Instant time = invoice.getPaidAt();
                
                RecentInvoiceResponse response = new RecentInvoiceResponse();
                response.setId(invoice.getId());
                response.setCode(invoice.getInvoiceCode());
                response.setTable(tableName);
                response.setTime(time);
                response.setSubtotal(invoice.getSubtotalAmount() != null ? invoice.getSubtotalAmount() : BigDecimal.ZERO);
                response.setDiscount(invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : BigDecimal.ZERO);
                response.setServiceFee(invoice.getServiceFeeAmount() != null ? invoice.getServiceFeeAmount() : BigDecimal.ZERO);
                response.setTax(invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO);
                response.setFinalAmount(invoice.getFinalAmount() != null ? invoice.getFinalAmount() : BigDecimal.ZERO);
                response.setStatus(invoice.getInvoiceStatus());
                response.setPaymentMethod(invoice.getPaymentMethod());
                
                // Set customer info
                if (invoice.getCustomer() != null) {
                    response.setCustomerName(invoice.getCustomer().getFullName());
                    response.setCustomerEmail(invoice.getCustomer().getEmail());
                    response.setCustomerPhone(invoice.getCustomer().getPhoneNumber());
                }
                
                return response;
            })
            .collect(Collectors.toList());
    }

    public List<com.example.datn_sd_29.dashboard.dto.TableStatusResponse> getTableStatus() {
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.systemDefault();
        
        // Get all tables
        List<com.example.datn_sd_29.dining_table.entity.DiningTable> allTables = diningTableRepository.findAll();
        
        // Get all active invoices (RESERVED and IN_PROGRESS)
        List<Invoice> activeInvoices = invoiceRepository.findAll().stream()
            .filter(i -> "RESERVED".equals(i.getInvoiceStatus()) || 
                        "IN_PROGRESS".equals(i.getInvoiceStatus()))
            .collect(Collectors.toList());
        
        // Build map of table ID to invoice
        Map<Integer, Invoice> tableToInvoiceMap = new HashMap<>();
        for (Invoice invoice : activeInvoices) {
            List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
            for (InvoiceDiningTable idt : invoiceTables) {
                if (idt.getDiningTable() != null) {
                    tableToInvoiceMap.put(idt.getDiningTable().getId(), invoice);
                }
            }
        }
        
        return allTables.stream()
            .map(table -> {
                String status = "AVAILABLE";
                Long minutesSinceCheckIn = null;
                Instant reservedAt = null;
                String customerName = null;
                
                // Check if table has an active invoice
                Invoice invoice = tableToInvoiceMap.get(table.getId());
                if (invoice != null) {
                    // Get customer name if available
                    if (invoice.getCustomer() != null) {
                        customerName = invoice.getCustomer().getFullName();
                    }
                    
                    if ("IN_PROGRESS".equals(invoice.getInvoiceStatus())) {
                        // Check if invoice has been in progress for more than 90 minutes
                        if (invoice.getCheckedInAt() != null) {
                            minutesSinceCheckIn = java.time.temporal.ChronoUnit.MINUTES.between(
                                invoice.getCheckedInAt(), now
                            );
                            
                            if (minutesSinceCheckIn <= 90) {
                                // Normal service time
                                status = "OCCUPIED";
                            } else {
                                // Over 90 minutes - needs attention
                                status = "OVERTIME";
                            }
                        } else {
                            // No check-in time, still show as OCCUPIED
                            status = "OCCUPIED";
                        }
                    } else if ("RESERVED".equals(invoice.getInvoiceStatus()) && invoice.getReservedAt() != null) {
                        // Check if reservation is within 2 hours from now
                        Instant reservationTime = invoice.getReservedAt()
                            .atZone(zoneId)
                            .toInstant();
                        
                        long hoursUntilReservation = java.time.temporal.ChronoUnit.HOURS.between(now, reservationTime);
                        
                        // Only show RESERVED if reservation is within 2 hours
                        if (hoursUntilReservation >= 0 && hoursUntilReservation <= 2) {
                            status = "RESERVED";
                            reservedAt = reservationTime;
                        }
                        // Otherwise, table shows as AVAILABLE (reservation is too far in the future)
                    }
                }
                
                return new com.example.datn_sd_29.dashboard.dto.TableStatusResponse(
                    table.getId(),
                    table.getTableName(),
                    table.getSeatingCapacity(),
                    status,
                    minutesSinceCheckIn,
                    reservedAt,
                    customerName
                );
            })
            .collect(Collectors.toList());
    }

    public com.example.datn_sd_29.dashboard.dto.TableDetailResponse getTableDetail(Integer tableId) {
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.systemDefault();
        
        // Get table
        com.example.datn_sd_29.dining_table.entity.DiningTable table = diningTableRepository.findById(tableId)
            .orElseThrow(() -> new RuntimeException("Table not found"));
        
        // Find active invoice for this table
        // Priority: IN_PROGRESS first, then RESERVED
        List<Invoice> activeInvoices = invoiceRepository.findAll().stream()
            .filter(i -> "RESERVED".equals(i.getInvoiceStatus()) || 
                        "IN_PROGRESS".equals(i.getInvoiceStatus()))
            .sorted((a, b) -> {
                // IN_PROGRESS has higher priority than RESERVED
                if ("IN_PROGRESS".equals(a.getInvoiceStatus()) && "RESERVED".equals(b.getInvoiceStatus())) {
                    return -1;
                }
                if ("RESERVED".equals(a.getInvoiceStatus()) && "IN_PROGRESS".equals(b.getInvoiceStatus())) {
                    return 1;
                }
                return 0;
            })
            .collect(Collectors.toList());
        
        Invoice invoice = null;
        List<String> combinedTableNames = new ArrayList<>();
        
        for (Invoice inv : activeInvoices) {
            List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository.findByInvoiceIdWithTable(inv.getId());
            boolean hasThisTable = invoiceTables.stream()
                .anyMatch(idt -> idt.getDiningTable() != null && idt.getDiningTable().getId().equals(tableId));
            
            if (hasThisTable) {
                invoice = inv;
                // Get all table names for this invoice (for combined tables display)
                // Sort by table ID (not table name) to ensure correct order
                combinedTableNames = invoiceTables.stream()
                    .filter(idt -> idt.getDiningTable() != null)
                    .sorted((a, b) -> a.getDiningTable().getId().compareTo(b.getDiningTable().getId()))
                    .map(idt -> idt.getDiningTable().getTableName())
                    .collect(Collectors.toList());
                break;
            }
        }
        
        // Calculate status
        String status = "AVAILABLE";
        if (invoice != null) {
            if ("IN_PROGRESS".equals(invoice.getInvoiceStatus())) {
                if (invoice.getCheckedInAt() != null) {
                    long minutesSinceCheckIn = java.time.temporal.ChronoUnit.MINUTES.between(
                        invoice.getCheckedInAt(), now
                    );
                    status = minutesSinceCheckIn <= 90 ? "OCCUPIED" : "OVERTIME";
                } else {
                    status = "OCCUPIED";
                }
            } else if ("RESERVED".equals(invoice.getInvoiceStatus()) && invoice.getReservedAt() != null) {
                Instant reservationTime = invoice.getReservedAt().atZone(zoneId).toInstant();
                long hoursUntilReservation = java.time.temporal.ChronoUnit.HOURS.between(now, reservationTime);
                if (hoursUntilReservation >= 0 && hoursUntilReservation <= 2) {
                    status = "RESERVED";
                }
            }
        }
        
        com.example.datn_sd_29.dashboard.dto.TableDetailResponse response = 
            new com.example.datn_sd_29.dashboard.dto.TableDetailResponse();
        response.setTableId(table.getId());
        
        // If combined tables, show all table names
        if (combinedTableNames.size() > 1) {
            response.setTableName(String.join(" + ", combinedTableNames));
        } else {
            response.setTableName(table.getTableName());
        }
        
        response.setCapacity(table.getSeatingCapacity());
        response.setStatus(status);
        
        if (invoice != null && !"AVAILABLE".equals(status)) {
            response.setInvoiceId(invoice.getId());
            response.setInvoiceCode(invoice.getInvoiceCode());
            response.setInvoiceStatus(invoice.getInvoiceStatus());
            response.setCheckedInAt(invoice.getCheckedInAt());
            response.setReservedAt(invoice.getReservedAt() != null ? 
                invoice.getReservedAt().atZone(zoneId).toInstant() : null);
            response.setGuestCount(invoice.getGuestCount());
            response.setSubtotal(invoice.getSubtotalAmount());
            response.setFinalAmount(invoice.getFinalAmount());
            
            if (invoice.getCheckedInAt() != null) {
                response.setMinutesSinceCheckIn(
                    java.time.temporal.ChronoUnit.MINUTES.between(invoice.getCheckedInAt(), now)
                );
            }
            
            if (invoice.getCustomer() != null) {
                response.setCustomerName(invoice.getCustomer().getFullName());
                response.setCustomerPhone(invoice.getCustomer().getPhoneNumber());
            }
            
            if (invoice.getEmployee() != null) {
                response.setStaffName(invoice.getEmployee().getFullName());
            }
        }
        
        return response;
    }

    private Double calculatePercentChange(BigDecimal today, BigDecimal yesterday) {
        if (yesterday == null || yesterday.compareTo(BigDecimal.ZERO) == 0) {
            return today != null && today.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        if (today == null) today = BigDecimal.ZERO;
        
        return today.subtract(yesterday)
            .divide(yesterday, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .doubleValue();
    }

    private Double calculatePercentChange(Long today, Long yesterday) {
        if (yesterday == null || yesterday == 0) {
            return today != null && today > 0 ? 100.0 : 0.0;
        }
        if (today == null) today = 0L;
        
        return ((today - yesterday) * 100.0) / yesterday;
    }

    private String getTrend(BigDecimal today, BigDecimal yesterday) {
        if (today == null) today = BigDecimal.ZERO;
        if (yesterday == null) yesterday = BigDecimal.ZERO;
        
        int comparison = today.compareTo(yesterday);
        if (comparison > 0) return "up";
        if (comparison < 0) return "down";
        return "neutral";
    }

    private String getTrend(Long today, Long yesterday) {
        if (today == null) today = 0L;
        if (yesterday == null) yesterday = 0L;
        
        if (today > yesterday) return "up";
        if (today < yesterday) return "down";
        return "neutral";
    }
    
    public com.example.datn_sd_29.dashboard.dto.RevenueChartResponse getRevenueChart(LocalDate startDate, LocalDate endDate) {
        ZoneId zoneId = ZoneId.systemDefault();
        
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();
        
        // Get all paid invoices
        List<Invoice> allInvoices = invoiceRepository.findAll();
        
        // Iterate through each day in the range
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            Instant dayStart = currentDate.atStartOfDay(zoneId).toInstant();
            Instant dayEnd = currentDate.plusDays(1).atStartOfDay(zoneId).toInstant();
            
            // Calculate revenue for this day
            BigDecimal dayRevenue = allInvoices.stream()
                .filter(i -> "PAID".equals(i.getInvoiceStatus()))
                .filter(i -> i.getPaidAt() != null)
                .filter(i -> !i.getPaidAt().isBefore(dayStart) && i.getPaidAt().isBefore(dayEnd))
                .map(Invoice::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Format label based on date range
            String label;
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            if (daysDiff <= 7) {
                // Show day of week for 7 days or less
                label = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE dd/MM", java.util.Locale.forLanguageTag("vi")));
            } else if (daysDiff <= 31) {
                // Show date for up to 31 days
                label = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
            } else {
                // Show date for longer periods
                label = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
            }
            
            labels.add(label);
            data.add(dayRevenue);
            
            currentDate = currentDate.plusDays(1);
        }
        
        return new com.example.datn_sd_29.dashboard.dto.RevenueChartResponse(labels, data);
    }

    // Helper class for grouping products
    private static class TopProductData {
        Integer id;
        String name;
        String category;
        int quantity = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        
        TopProductData(Integer id, String name, String category) {
            this.id = id;
            this.name = name;
            this.category = category;
        }
    }

    public InvoicePageResponse getAllInvoices(
            int page,
            int size,
            String status,
            LocalDate startDate,
            LocalDate endDate,
            String search,
            String paymentMethod,
            String sortBy,
            String sortDirection
    ) {
        ZoneId zoneId = ZoneId.systemDefault();
        Instant now = Instant.now();
        
        // Get all invoices
        List<Invoice> allInvoices = invoiceRepository.findAll();
        
        // Filter by status
        if (status != null && !status.isEmpty() && !"ALL".equals(status)) {
            allInvoices = allInvoices.stream()
                .filter(i -> status.equals(i.getInvoiceStatus()))
                .collect(Collectors.toList());
        }
        
        // Filter by date range
        if (startDate != null && endDate != null) {
            Instant startOfPeriod = startDate.atStartOfDay(zoneId).toInstant();
            Instant endOfPeriod = endDate.plusDays(1).atStartOfDay(zoneId).toInstant();
            
            allInvoices = allInvoices.stream()
                .filter(i -> {
                    Instant invoiceTime = i.getPaidAt() != null ? i.getPaidAt() : 
                                         i.getCheckedInAt() != null ? i.getCheckedInAt() :
                                         i.getReservedAt() != null ? i.getReservedAt().atZone(zoneId).toInstant() : null;
                    
                    return invoiceTime != null && 
                           !invoiceTime.isBefore(startOfPeriod) && 
                           invoiceTime.isBefore(endOfPeriod);
                })
                .collect(Collectors.toList());
        }
        
        // Filter by payment method
        if (paymentMethod != null && !paymentMethod.isEmpty() && !"ALL".equals(paymentMethod)) {
            allInvoices = allInvoices.stream()
                .filter(i -> paymentMethod.equals(i.getPaymentMethod()))
                .collect(Collectors.toList());
        }
        
        // Search
        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            allInvoices = allInvoices.stream()
                .filter(i -> {
                    // Search by invoice code
                    if (i.getInvoiceCode() != null && i.getInvoiceCode().toLowerCase().contains(searchLower)) {
                        return true;
                    }
                    
                    // Search by customer name
                    if (i.getCustomer() != null && i.getCustomer().getFullName() != null && 
                        i.getCustomer().getFullName().toLowerCase().contains(searchLower)) {
                        return true;
                    }
                    
                    // Search by customer phone
                    if (i.getCustomer() != null && i.getCustomer().getPhoneNumber() != null && 
                        i.getCustomer().getPhoneNumber().contains(searchLower)) {
                        return true;
                    }
                    
                    // Search by table name
                    List<InvoiceDiningTable> tables = invoiceDiningTableRepository.findByInvoiceIdWithTable(i.getId());
                    return tables.stream()
                        .anyMatch(idt -> idt.getDiningTable() != null && 
                                        idt.getDiningTable().getTableName().toLowerCase().contains(searchLower));
                })
                .collect(Collectors.toList());
        }
        
        // Sort
        allInvoices.sort((a, b) -> {
            Instant timeA = a.getPaidAt() != null ? a.getPaidAt() : 
                           a.getCheckedInAt() != null ? a.getCheckedInAt() :
                           a.getReservedAt() != null ? a.getReservedAt().atZone(zoneId).toInstant() : null;
            Instant timeB = b.getPaidAt() != null ? b.getPaidAt() : 
                           b.getCheckedInAt() != null ? b.getCheckedInAt() :
                           b.getReservedAt() != null ? b.getReservedAt().atZone(zoneId).toInstant() : null;
            
            if ("finalAmount".equals(sortBy)) {
                BigDecimal amountA = a.getFinalAmount() != null ? a.getFinalAmount() : BigDecimal.ZERO;
                BigDecimal amountB = b.getFinalAmount() != null ? b.getFinalAmount() : BigDecimal.ZERO;
                return "asc".equals(sortDirection) ? amountA.compareTo(amountB) : amountB.compareTo(amountA);
            }
            
            // Default sort by time
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return "asc".equals(sortDirection) ? timeA.compareTo(timeB) : timeB.compareTo(timeA);
        });
        
        // Calculate pagination
        int totalElements = allInvoices.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalElements);
        
        // Get page content
        List<Invoice> pageInvoices = startIndex < totalElements ? 
            allInvoices.subList(startIndex, endIndex) : new ArrayList<>();
        
        // Map to response
        List<RecentInvoiceResponse> content = pageInvoices.stream()
            .map(invoice -> {
                String tableName = "N/A";
                List<InvoiceDiningTable> tables = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
                if (tables != null && !tables.isEmpty()) {
                    tableName = tables.stream()
                        .filter(idt -> idt.getDiningTable() != null)
                        .sorted((a, b) -> a.getDiningTable().getId().compareTo(b.getDiningTable().getId()))
                        .map(idt -> idt.getDiningTable().getTableName())
                        .collect(Collectors.joining(" + "));
                    
                    if (tableName.isEmpty()) {
                        tableName = "N/A";
                    }
                }
                
                Instant time = invoice.getPaidAt() != null ? invoice.getPaidAt() : 
                              invoice.getCheckedInAt() != null ? invoice.getCheckedInAt() :
                              invoice.getReservedAt() != null ? invoice.getReservedAt().atZone(zoneId).toInstant() : null;
                
                RecentInvoiceResponse response = new RecentInvoiceResponse();
                response.setId(invoice.getId());
                response.setCode(invoice.getInvoiceCode());
                response.setTable(tableName);
                response.setTime(time);
                response.setSubtotal(invoice.getSubtotalAmount() != null ? invoice.getSubtotalAmount() : BigDecimal.ZERO);
                response.setDiscount(invoice.getDiscountAmount() != null ? invoice.getDiscountAmount() : BigDecimal.ZERO);
                response.setServiceFee(invoice.getServiceFeeAmount() != null ? invoice.getServiceFeeAmount() : BigDecimal.ZERO);
                response.setTax(invoice.getTaxAmount() != null ? invoice.getTaxAmount() : BigDecimal.ZERO);
                response.setFinalAmount(invoice.getFinalAmount() != null ? invoice.getFinalAmount() : BigDecimal.ZERO);
                response.setStatus(invoice.getInvoiceStatus());
                response.setPaymentMethod(invoice.getPaymentMethod());
                
                // Set customer info
                if (invoice.getCustomer() != null) {
                    response.setCustomerName(invoice.getCustomer().getFullName());
                    response.setCustomerEmail(invoice.getCustomer().getEmail());
                    response.setCustomerPhone(invoice.getCustomer().getPhoneNumber());
                }
                
                return response;
            })
            .collect(Collectors.toList());
        
        return new InvoicePageResponse(content, totalElements, totalPages, page, size);
    }

    public com.example.datn_sd_29.invoice.dto.PaymentDetailResponse getInvoiceDetail(Integer invoiceId) {
        // Find invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        
        // Get invoice items
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithItem(invoiceId);
        
        // Get tables
        List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoiceId);
        
        // Build response
        com.example.datn_sd_29.invoice.dto.PaymentDetailResponse response = 
            new com.example.datn_sd_29.invoice.dto.PaymentDetailResponse();
        
        response.setInvoiceId(invoice.getId());
        response.setInvoiceCode(invoice.getInvoiceCode());
        response.setInvoiceStatus(invoice.getInvoiceStatus());
        response.setGuestCount(invoice.getGuestCount());
        response.setCheckedInAt(invoice.getCheckedInAt());
        response.setReservedAt(invoice.getReservedAt());
        response.setSubtotal(invoice.getSubtotalAmount());
        response.setManualDiscountPercent(invoice.getManualDiscountPercent());
        response.setManualDiscountAmount(invoice.getManualDiscountAmount());
        response.setTaxPercent(invoice.getTaxPercent());
        response.setServiceFeePercent(invoice.getServiceFeePercent());
        
        // Customer info
        if (invoice.getCustomer() != null) {
            response.setCustomerType(invoice.getCustomer().getIsActive() ? "MEMBER" : "GUEST");
            response.setCustomerName(invoice.getCustomer().getFullName());
            response.setCustomerPhone(invoice.getCustomer().getPhoneNumber());
            response.setLoyaltyPoints(invoice.getCustomer().getLoyaltyPoints());
        }
        
        // Staff info
        if (invoice.getEmployee() != null) {
            response.setStaffName(invoice.getEmployee().getFullName());
        }
        
        // Tables
        List<com.example.datn_sd_29.invoice.dto.PaymentDetailResponse.TableSummary> tableSummaries = 
            invoiceTables.stream()
                .filter(idt -> idt.getDiningTable() != null)
                .map(idt -> {
                    com.example.datn_sd_29.invoice.dto.PaymentDetailResponse.TableSummary summary = 
                        new com.example.datn_sd_29.invoice.dto.PaymentDetailResponse.TableSummary();
                    summary.setId(idt.getDiningTable().getId());
                    summary.setTableName(idt.getDiningTable().getTableName());
                    summary.setSeatingCapacity(idt.getDiningTable().getSeatingCapacity());
                    return summary;
                })
                .collect(Collectors.toList());
        response.setTables(tableSummaries);
        
        // Items
        List<com.example.datn_sd_29.invoice.dto.PaymentItemResponse> itemResponses = 
            items.stream()
                .map(item -> {
                    com.example.datn_sd_29.invoice.dto.PaymentItemResponse itemResponse = 
                        new com.example.datn_sd_29.invoice.dto.PaymentItemResponse();
                    itemResponse.setId(item.getId());
                    
                    // Check if it's a product or combo
                    if (item.getProduct() != null) {
                        itemResponse.setProductId(item.getProduct().getId());
                        itemResponse.setName(item.getProduct().getProductName());
                        itemResponse.setType("PRODUCT");
                    } else if (item.getProductCombo() != null) {
                        itemResponse.setComboId(item.getProductCombo().getId());
                        itemResponse.setName(item.getProductCombo().getComboName());
                        itemResponse.setType("COMBO");
                    } else {
                        itemResponse.setName("N/A");
                        itemResponse.setType("UNKNOWN");
                    }
                    
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setUnitPrice(item.getUnitPrice());
                    itemResponse.setDiscount(BigDecimal.ZERO);
                    itemResponse.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                    return itemResponse;
                })
                .collect(Collectors.toList());
        response.setItems(itemResponses);
        
        // Vouchers (empty for now)
        response.setVouchers(new ArrayList<>());
        
        return response;
    }
}
