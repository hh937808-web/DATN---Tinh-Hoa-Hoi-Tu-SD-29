package com.example.datn_sd_29.walkin.service;

import com.example.datn_sd_29.common.service.TableStatusBroadcastService;
import com.example.datn_sd_29.common.service.InvoiceBroadcastService;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.repository.EmployeeRepository;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.walkin.dto.SuggestedTablesResponse;
import com.example.datn_sd_29.walkin.dto.WalkInCheckInRequest;
import com.example.datn_sd_29.walkin.dto.WalkInCheckInResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkInService {
    
    private final InvoiceRepository invoiceRepository;
    private final DiningTableRepository diningTableRepository;
    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final TableStatusBroadcastService tableStatusBroadcastService;
    private final InvoiceBroadcastService invoiceBroadcastService;
    
    @org.springframework.beans.factory.annotation.Value("${security.api.enabled:true}")
    private boolean securityEnabled;
    
    // Area-based scoring constants - OPTIMIZED FOR WALK-IN
    private static final int SINGLE_AREA_COMPLETE_BONUS = 10000; // CRITICAL: Highest priority - one area can handle all guests
    private static final int SINGLE_TABLE_BONUS = 5000; // CRITICAL: Single table ALWAYS preferred over multi-table combos
    private static final int FLOOR_1_BONUS = 2000;
    private static final int FLOOR_2_BONUS = 1000;
    private static final int AREA_A_BONUS = 800; // Increased - prioritize Area A more
    private static final int SAME_AREA_BONUS = 50; // REDUCED FURTHER - minimize multi-table combo bonus
    private static final int CONSECUTIVE_ID_BONUS = 10; // REDUCED FURTHER - don't over-prioritize consecutive IDs
    private static final int ID_DISTANCE_PENALTY = 50; // Increased - strongly penalize distant tables
    private static final int TABLE_COUNT_PENALTY = 500; // INCREASED DRAMATICALLY - very strongly prefer fewer tables
    private static final int EXCESS_CAPACITY_PENALTY = 50; // INCREASED DRAMATICALLY - very strongly penalize wasted seats
    private static final int MAX_TABLES_TO_COMBINE = 5; // Increased from 4 to 5 for E1-E5

    @Transactional
    public WalkInCheckInResponse checkInWalkIn(WalkInCheckInRequest request) {
        // Validate authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Nếu security bị tắt, bỏ qua authentication check
        if (securityEnabled && (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null
            || "anonymousUser".equals(auth.getPrincipal()))) {
            throw new IllegalArgumentException("Vui lòng đăng nhập lại tài khoản!");
        }

        // Get employee based on security mode
        Employee employee;
        if (securityEnabled) {
            // Production mode: lấy employee từ JWT token
            // JWT subject cho employee là USERNAME (không phải email)
            String username = auth.getPrincipal().toString();
            employee = employeeRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        } else {
            // Development mode: lấy employee từ X-Employee-Username header
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest httpRequest = attributes.getRequest();
                    String username = httpRequest.getHeader("X-Employee-Username");
                    
                    if (username != null && !username.trim().isEmpty()) {
                        employee = employeeRepository.findByUsernameIgnoreCase(username.trim())
                                .orElseThrow(() -> new IllegalArgumentException("Employee not found with username: " + username));
                    } else {
                        throw new IllegalArgumentException("X-Employee-Username header is required when security is disabled");
                    }
                } else {
                    throw new IllegalArgumentException("Cannot get request context");
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to get employee from header: " + e.getMessage());
            }
        }

        // Validate request
        if (request.getTableIds() == null || request.getTableIds().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn bàn");
        }

        if (request.getGuestCount() == null || request.getGuestCount() < 1) {
            throw new IllegalArgumentException("Số khách phải lớn hơn 0");
        }

        // Get tables
        List<DiningTable> tables = diningTableRepository.findAllById(request.getTableIds());
        if (tables.size() != request.getTableIds().size()) {
            throw new IllegalArgumentException("Một số bàn không tồn tại");
        }

        // Validate capacity: guest count must not exceed total table capacity
        int totalCapacity = tables.stream()
                .mapToInt(DiningTable::getSeatingCapacity)
                .sum();
        
        if (request.getGuestCount() > totalCapacity) {
            throw new IllegalArgumentException(
                String.format("Số khách (%d) vượt quá sức chứa của bàn (tối đa %d chỗ)", 
                    request.getGuestCount(), totalCapacity)
            );
        }

        // Check for conflicts BEFORE creating new invoice - fail explicitly if table in use
        for (DiningTable table : tables) {
            final Integer tableId = table.getId();
            
            List<Invoice> conflictingInvoices = invoiceDiningTableRepository
                    .findDistinctInvoicesByTableAndStatuses(
                            tableId,
                            List.of("IN_PROGRESS", "RESERVED")
                    );

            if (!conflictingInvoices.isEmpty()) {
                Invoice conflict = conflictingInvoices.get(0);
                throw new IllegalStateException(
                    String.format("Bàn %s đã có hóa đơn %s (trạng thái: %s). Vui lòng chọn bàn khác hoặc hủy hóa đơn hiện tại.",
                        table.getTableName(),
                        conflict.getInvoiceCode(),
                        conflict.getInvoiceStatus())
                );
            }
        }

        // Create new invoice
        Invoice invoice = new Invoice();
        invoice.setEmployee(employee);
        invoice.setInvoiceChannel("OFFLINE");
        invoice.setInvoiceStatus("IN_PROGRESS");
        invoice.setInvoiceCode("INV-" + UUID.randomUUID());
        invoice.setCheckedInAt(Instant.now());
        invoice.setGuestCount(request.getGuestCount());
        
        // For walk-in customers, save name to guestName field (not create Customer entity)
        if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
            invoice.setGuestName(request.getCustomerName().trim());
        }
        
        invoice = invoiceRepository.save(invoice);

        // Link tables to invoice
        for (DiningTable table : tables) {
            InvoiceDiningTable idt = new InvoiceDiningTable();
            idt.setInvoice(invoice);
            idt.setDiningTable(table);
            invoiceDiningTableRepository.save(idt);
        }

        // Update table status to IN_USE
        List<Integer> tableIds = tables.stream()
                .map(DiningTable::getId)
                .collect(Collectors.toList());
        
        if (!tableIds.isEmpty()) {
            diningTableRepository.updateTableStatusByIdIn(tableIds, "IN_USE");
            tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "IN_USE");
        }

        // Broadcast invoice update
        invoiceBroadcastService.broadcastInvoiceUpdate(
            invoice.getId(),
            invoice.getInvoiceCode(),
            "IN_PROGRESS"
        );

        return new WalkInCheckInResponse(
            invoice.getId(),
            invoice.getInvoiceCode(),
            "Check-in thành công"
        );
    }

    @Transactional
    public void cancelTable(String invoiceCode) {
        // Find invoice by code
        Invoice invoice = invoiceRepository.findByInvoiceCode(invoiceCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));

        // Only allow cancelling IN_PROGRESS invoices
        if (!"IN_PROGRESS".equals(invoice.getInvoiceStatus())) {
            throw new IllegalArgumentException("Chỉ có thể hủy hóa đơn đang phục vụ");
        }

        // Get all tables linked to this invoice
        List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
        List<Integer> tableIds = invoiceTables.stream()
                .map(idt -> idt.getDiningTable().getId())
                .collect(Collectors.toList());

        // Cancel invoice
        invoice.setInvoiceStatus("CANCELLED");
        invoiceRepository.save(invoice);

        // Set tables back to AVAILABLE
        if (!tableIds.isEmpty()) {
            diningTableRepository.updateTableStatusByIdIn(tableIds, "AVAILABLE");
            tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "AVAILABLE");
        }
    }
    
    /**
     * Suggests best tables for walk-in guests using area-based scoring
     * Same algorithm as online reservation
     */
    public SuggestedTablesResponse suggestTablesForWalkIn(Integer guestCount) {
        if (guestCount == null || guestCount < 1) {
            throw new IllegalArgumentException("Số khách phải lớn hơn 0");
        }
        
        // Get all available tables (not IN_USE, not RESERVED, not OUT_OF_SERVICE)
        List<DiningTable> availableTables = getAvailableTablesForWalkIn();
        
        if (availableTables.isEmpty()) {
            return new SuggestedTablesResponse(
                List.of(),
                0,
                "Hiện tại không có bàn trống"
            );
        }
        
        // Use area-based scoring to select best tables
        List<DiningTable> selectedTables = selectTablesForWalkIn(availableTables, guestCount);
        
        if (selectedTables.isEmpty()) {
            return new SuggestedTablesResponse(
                List.of(),
                0,
                "Không tìm được bàn phù hợp cho " + guestCount + " khách"
            );
        }
        
        // Convert to response
        List<SuggestedTablesResponse.TableInfo> tableInfos = selectedTables.stream()
                .map(table -> new SuggestedTablesResponse.TableInfo(
                        table.getId(),
                        table.getTableName(),
                        table.getSeatingCapacity(),
                        table.getArea(),
                        table.getFloor()
                ))
                .collect(Collectors.toList());
        
        int totalCapacity = selectedTables.stream()
                .mapToInt(DiningTable::getSeatingCapacity)
                .sum();
        
        String message = String.format("Gợi ý %d bàn (tổng %d chỗ) cho %d khách", 
                selectedTables.size(), totalCapacity, guestCount);
        
        return new SuggestedTablesResponse(tableInfos, totalCapacity, message);
    }
    
    /**
     * Get all available tables for walk-in (real-time availability)
     */
    private List<DiningTable> getAvailableTablesForWalkIn() {
        // Get all tables
        List<DiningTable> allTables = diningTableRepository.findBySeatingCapacityGreaterThanEqual(1);
        
        return allTables.stream()
                .filter(this::isTableServiceable)
                .filter(table -> {
                    // Check if table has any active invoice (IN_PROGRESS or RESERVED)
                    List<Invoice> activeInvoices = invoiceDiningTableRepository
                            .findDistinctInvoicesByTableAndStatuses(
                                    table.getId(),
                                    List.of("IN_PROGRESS", "RESERVED")
                            );
                    return activeInvoices.isEmpty();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Select best tables using area-based scoring (same as ReservationService)
     */
    private List<DiningTable> selectTablesForWalkIn(List<DiningTable> tables, int guestCount) {
        if (tables.isEmpty()) {
            return List.of();
        }
        
        // Try area-based scoring first
        List<DiningTable> selected = trySelectWithAreaScoring(tables, guestCount);
        if (!selected.isEmpty()) {
            return selected;
        }
        
        // Fallback: try greedy approach
        return trySelectFromTables(tables, guestCount);
    }
    
    /**
     * Area-based scoring system
     */
    private List<DiningTable> trySelectWithAreaScoring(List<DiningTable> tables, int guestCount) {
        if (tables.isEmpty()) {
            return List.of();
        }
        
        // Group tables by area
        var tablesByArea = tables.stream()
                .filter(t -> t.getArea() != null && t.getFloor() != null)
                .collect(Collectors.groupingBy(
                    DiningTable::getArea,
                    Collectors.toList()
                ));
        
        // PRIORITY 1: Try to find a SINGLE AREA that can accommodate all guests
        String bestArea = null;
        List<DiningTable> bestCombo = List.of();
        int bestScore = Integer.MIN_VALUE;
        
        for (var entry : tablesByArea.entrySet()) {
            String area = entry.getKey();
            List<DiningTable> areaTables = entry.getValue();
            areaTables.sort(Comparator.comparingInt(DiningTable::getId));
            
            // Find best combo in this area
            var result = findBestComboInArea(areaTables, guestCount);
            if (!result.isEmpty()) {
                int score = calculateAreaScore(result, guestCount);
                if (score > bestScore) {
                    bestScore = score;
                    bestCombo = result;
                    bestArea = area;
                }
            }
        }
        
        // If we found a single-area solution, return it immediately
        if (!bestCombo.isEmpty()) {
            log.info("Walk-in: Selected {} tables in area {} with score {} for {} guests", 
                     bestCombo.size(), bestArea, bestScore, guestCount);
            return bestCombo;
        }
        
        // PRIORITY 2: If no single area works, try same floor (multiple areas allowed)
        log.warn("Walk-in: No single area can accommodate {} guests, trying same-floor multi-area", guestCount);
        
        var tablesByFloor = tables.stream()
                .filter(t -> t.getFloor() != null)
                .collect(Collectors.groupingBy(
                    DiningTable::getFloor,
                    Collectors.toList()
                ));
        
        // Try floor 1 first, then floor 2
        for (Integer floor : List.of(1, 2)) {
            List<DiningTable> floorTables = tablesByFloor.get(floor);
            if (floorTables != null && !floorTables.isEmpty()) {
                floorTables.sort(Comparator.comparingInt(DiningTable::getId));
                
                List<DiningTable> combo = trySelectFromTables(floorTables, guestCount);
                if (!combo.isEmpty()) {
                    log.info("Walk-in: Selected {} tables on floor {} (multi-area fallback) for {} guests", 
                             combo.size(), floor, guestCount);
                    return combo;
                }
            }
        }
        
        // CRITICAL: NEVER allow cross-floor combination
        // Return empty list if no same-floor solution found
        log.warn("Walk-in: Cannot find same-floor tables for {} guests - returning empty", guestCount);
        return List.of();
    }
    
    /**
     * Find best combo in a single area
     * CRITICAL: Single table ALWAYS wins if capacity is sufficient
     */
    private List<DiningTable> findBestComboInArea(List<DiningTable> areaTables, int guestCount) {
        // PRIORITY 1: Try single table first - if found, return immediately
        for (DiningTable table : areaTables) {
            int capacity = capacitySafe(table);
            if (capacity >= guestCount) {
                // Found a single table that fits - return immediately without checking combos
                return List.of(table);
            }
        }
        
        // PRIORITY 2: No single table fits, try combinations of 2-5 tables
        return findBestMultiTableComboInArea(areaTables, guestCount);
    }
    
    /**
     * Find best MULTI-TABLE combo in a single area (skip single table check)
     * Used when we already know no single table is sufficient
     */
    private List<DiningTable> findBestMultiTableComboInArea(List<DiningTable> areaTables, int guestCount) {
        List<DiningTable> bestCombo = List.of();
        int bestScore = Integer.MIN_VALUE;
        
        for (int k = 2; k <= Math.min(MAX_TABLES_TO_COMBINE, areaTables.size()); k++) {
            List<DiningTable> combo = findBestCombo(areaTables, guestCount, k);
            if (!combo.isEmpty()) {
                // CRITICAL: Only accept combo if it has enough capacity
                int totalCapacity = combo.stream().mapToInt(this::capacitySafe).sum();
                if (totalCapacity >= guestCount) {
                    int score = calculateAreaScore(combo, guestCount);
                    if (score > bestScore) {
                        bestScore = score;
                        bestCombo = combo;
                    }
                }
            }
        }
        
        return bestCombo;
    }
    
    /**
     * Calculate area score
     * 
     * PRIORITY HIERARCHY:
     * 1. SINGLE AREA COMPLETENESS (+10000) - If one area can handle all guests, it ALWAYS wins
     * 2. Floor preference (+2000 floor 1, +1000 floor 2)
     * 3. Area A bonus (+500)
     * 4. Consecutive IDs (+50 per table)
     * 5. Single table (+300)
     * 6. Penalties: ID distance, table count, excess capacity
     */
    private int calculateAreaScore(List<DiningTable> tables, int guestCount) {
        if (tables.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        
        int score = 0;
        
        // CRITICAL: Single area completeness bonus - HIGHEST PRIORITY
        // If all tables are in the same area AND capacity is sufficient, this should always win
        boolean allSameArea = tables.stream()
                .map(DiningTable::getArea)
                .distinct()
                .count() == 1;
        
        int totalCapacity = tables.stream().mapToInt(this::capacitySafe).sum();
        
        if (allSameArea && totalCapacity >= guestCount) {
            score += SINGLE_AREA_COMPLETE_BONUS;
        }
        
        // Floor bonus
        Integer floor = tables.get(0).getFloor();
        if (floor != null) {
            if (floor == 1) {
                score += FLOOR_1_BONUS;
            } else if (floor == 2) {
                score += FLOOR_2_BONUS;
            }
        }
        
        // Area A bonus
        String area = tables.get(0).getArea();
        if ("A".equals(area)) {
            score += AREA_A_BONUS;
        }
        
        // NEW: Same area bonus - all tables in same area (for multi-table combos)
        if (tables.size() > 1 && allSameArea) {
            score += SAME_AREA_BONUS * tables.size(); // More tables in same area = more bonus
        }
        
        // Single table bonus
        if (tables.size() == 1) {
            score += SINGLE_TABLE_BONUS;
        }
        
        // Bonus + phạt khoảng cách dựa vào số trong tableName (VD "A1","A2","A3"
        // được coi là sát nhau). KHÔNG dùng ID vì ID không phản ánh vị trí vật lý
        // (admin xóa rồi thêm lại bàn → ID mới có thể chen vào giữa).
        if (tables.size() > 1) {
            List<Integer> nums = tables.stream()
                    .map(WalkInService::extractTableNumber)
                    .filter(java.util.Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());

            // Chỉ áp dụng khi parse được tên của tất cả bàn
            if (nums.size() == tables.size()) {
                boolean allConsecutive = true;
                for (int i = 1; i < nums.size(); i++) {
                    if (nums.get(i) != nums.get(i - 1) + 1) {
                        allConsecutive = false;
                        break;
                    }
                }
                if (allConsecutive) {
                    score += CONSECUTIVE_ID_BONUS * tables.size();
                }

                int min = nums.get(0);
                int max = nums.get(nums.size() - 1);
                score -= (max - min) * ID_DISTANCE_PENALTY;
            }
        }
        
        // Table count penalty
        score -= tables.size() * TABLE_COUNT_PENALTY;
        
        // Excess capacity penalty
        int excessCapacity = Math.abs(totalCapacity - guestCount);
        score -= excessCapacity * EXCESS_CAPACITY_PENALTY;
        
        return score;
    }
    
    /**
     * Greedy fallback
     */
    private List<DiningTable> trySelectFromTables(List<DiningTable> tables, int guestCount) {
        if (tables.isEmpty()) {
            return List.of();
        }
        
        List<DiningTable> sorted = new ArrayList<>(tables);
        sorted.sort(Comparator.comparingInt(this::capacitySafe));
        
        // Try single table first
        for (DiningTable table : sorted) {
            if (capacitySafe(table) >= guestCount) {
                return List.of(table);
            }
        }
        
        // Try combinations
        int maxTables = Math.min(MAX_TABLES_TO_COMBINE, sorted.size());
        for (int k = 2; k <= maxTables; k++) {
            List<DiningTable> best = findBestCombo(sorted, guestCount, k);
            if (!best.isEmpty()) {
                return best;
            }
        }
        
        // Greedy approach as last resort - MUST be same floor
        List<DiningTable> greedy = new ArrayList<>();
        int total = 0;
        for (int i = sorted.size() - 1; i >= 0 && total < guestCount; i--) {
            DiningTable t = sorted.get(i);
            
            // CRITICAL: Only add table if same floor as already picked tables
            if (!greedy.isEmpty() && !isSameFloor(greedy.get(0), t)) {
                continue; // Skip this table - different floor
            }
            
            greedy.add(t);
            total += capacitySafe(t);
        }
        
        return total >= guestCount ? greedy : List.of();
    }
    
    /**
     * Find best combo using backtracking with SCORING (MUST be same floor)
     * CRITICAL: Use scoring system instead of just minimal capacity
     */
    private List<DiningTable> findBestCombo(List<DiningTable> tables, int guestCount, int k) {
        List<DiningTable>[] best = new List[]{List.of()};
        int[] bestScore = new int[]{Integer.MIN_VALUE};
        
        backtrackWithScoring(tables, guestCount, k, 0, new ArrayList<>(), 0, best, bestScore);
        return best[0];
    }
    
    private void backtrackWithScoring(
            List<DiningTable> tables,
            int guestCount,
            int k,
            int start,
            List<DiningTable> picked,
            int totalCap,
            List<DiningTable>[] best,
            int[] bestScore
    ) {
        if (picked.size() == k) {
            // CRITICAL: Validate all tables are on same floor AND have enough capacity
            if (totalCap >= guestCount && allSameFloor(picked)) {
                // Calculate score for this combo
                int score = calculateAreaScore(picked, guestCount);
                
                // Keep combo with HIGHEST score (not lowest capacity)
                if (score > bestScore[0]) {
                    bestScore[0] = score;
                    best[0] = new ArrayList<>(picked);
                }
            }
            return;
        }
        
        for (int i = start; i < tables.size(); i++) {
            DiningTable t = tables.get(i);
            
            // CRITICAL: Only pick table if it's same floor as already picked tables
            if (!picked.isEmpty() && !isSameFloor(picked.get(0), t)) {
                continue; // Skip this table - different floor
            }
            
            picked.add(t);
            backtrackWithScoring(tables, guestCount, k, i + 1, picked, totalCap + capacitySafe(t), best, bestScore);
            picked.remove(picked.size() - 1);
        }
    }
    
    /**
     * Check if all tables in list are on same floor
     */
    private boolean allSameFloor(List<DiningTable> tables) {
        if (tables.isEmpty()) {
            return true;
        }
        Integer firstFloor = tables.get(0).getFloor();
        if (firstFloor == null) {
            return false; // Cannot validate floor
        }
        return tables.stream().allMatch(t -> firstFloor.equals(t.getFloor()));
    }
    
    /**
     * Check if two tables are on same floor
     */
    private boolean isSameFloor(DiningTable t1, DiningTable t2) {
        if (t1.getFloor() == null || t2.getFloor() == null) {
            return false;
        }
        return t1.getFloor().equals(t2.getFloor());
    }
    
    private boolean isTableServiceable(DiningTable table) {
        String status = table.getTableStatus();
        return status == null || !"OUT_OF_SERVICE".equals(status);
    }
    
    private int capacitySafe(DiningTable table) {
        return table.getSeatingCapacity() == null ? 0 : table.getSeatingCapacity();
    }

    // Lấy phần số ở cuối tableName: "A1" → 1, "B12" → 12, "Bàn-3" → 3.
    // Trả về null nếu tên rỗng hoặc không có số.
    private static final java.util.regex.Pattern TRAILING_DIGITS = java.util.regex.Pattern.compile("(\\d+)$");

    private static Integer extractTableNumber(DiningTable t) {
        if (t == null || t.getTableName() == null) return null;
        java.util.regex.Matcher m = TRAILING_DIGITS.matcher(t.getTableName());
        if (!m.find()) return null;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
