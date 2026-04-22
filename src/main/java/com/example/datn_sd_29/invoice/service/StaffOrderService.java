package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import com.example.datn_sd_29.employee.entity.Employee;
import com.example.datn_sd_29.employee.repository.EmployeeRepository;
import com.example.datn_sd_29.invoice.dto.InvoiceGroupResponse;
import com.example.datn_sd_29.invoice.dto.InvoiceItemResponse;
import com.example.datn_sd_29.invoice.dto.OrderItemRequest;
import com.example.datn_sd_29.invoice.dto.StaffTableResponse;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoiceItemStatus;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product.enums.ProductCategory;
import com.example.datn_sd_29.product.repository.ProductRepository;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import com.example.datn_sd_29.product_combo.repository.ProductComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffOrderService {
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String TABLE_STATUS_IN_USE = "IN_USE";

    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final ProductRepository productRepository;
    private final ProductComboRepository productComboRepository;
    private final DiningTableRepository diningTableRepository;
    private final EmployeeRepository employeeRepository;
    private final com.example.datn_sd_29.common.service.KitchenBroadcastService kitchenBroadcastService;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.example.datn_sd_29.product_version.service.ProductVersionService productVersionService;
    
    @org.springframework.beans.factory.annotation.Value("${security.api.enabled:true}")
    private boolean securityEnabled;

    public List<InvoiceGroupResponse> getInProgressInvoices() {
        List<Invoice> invoices = invoiceRepository.findAllInProgressInvoicesWithCustomer();
        
        return invoices.stream()
                .map(this::mapToInvoiceGroupResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get current employee from authentication context
     * 
     * Behavior:
     * - If security is ENABLED: Get employee from JWT token
     * - If security is DISABLED: Get employee from X-Employee-Username header
     */
    private Employee getCurrentEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // If security is DISABLED, get employee from custom header
        if (!securityEnabled) {
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String username = request.getHeader("X-Employee-Username");
                    
                    System.out.println("🔍 DEBUG getCurrentEmployee() - Security DISABLED");
                    System.out.println("   X-Employee-Username header: " + username);
                    
                    if (username != null && !username.trim().isEmpty()) {
                        Employee emp = employeeRepository.findByUsernameIgnoreCase(username.trim()).orElse(null);
                        System.out.println("   Found employee: " + (emp != null ? emp.getFullName() + " (ID: " + emp.getId() + ")" : "NULL"));
                        return emp;
                    } else {
                        System.out.println("   ❌ Header is null or empty!");
                    }
                } else {
                    System.out.println("   ❌ RequestAttributes is null!");
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to get employee from header: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
        
        // If security is ENABLED but no authentication, return null
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        
        // Get employee from JWT token (JWT stores username as subject for employees)
        String username = auth.getName();
        return employeeRepository.findByUsernameIgnoreCase(username).orElse(null);
    }
    
    /**
     * Assign employee to invoice if not already assigned
     * Check if current employee has permission to order for this invoice
     * 
     * Behavior:
     * - If security is DISABLED: Always assign employee if available, but never block
     * - If security is ENABLED: Assign employee and enforce access control
     */
    private void assignAndCheckEmployee(Invoice invoice) {
        Employee currentEmployee = getCurrentEmployee();
        
        // If no authentication (no employee logged in), allow operation
        if (currentEmployee == null) {
            return;
        }
        
        // If invoice has no employee assigned, assign current employee
        if (invoice.getEmployee() == null) {
            invoice.setEmployee(currentEmployee);
            invoiceRepository.save(invoice);
            return;
        }
        
        // If invoice already has an employee assigned
        if (!invoice.getEmployee().getId().equals(currentEmployee.getId())) {
            // If security is DISABLED, allow operation (don't block)
            if (!securityEnabled) {
                return;
            }
            
            // If security is ENABLED, block the operation
            String assignedStaffName = invoice.getEmployee().getFullName();
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Bàn này đang được phục vụ bởi nhân viên " + assignedStaffName + ". " +
                "Chỉ nhân viên đã order mới có thể tiếp tục order cho bàn này."
            );
        }
    }
    
    /**
     * Validate and claim table for current staff (using serving_staff_id)
     * 
     * Rules:
     * - If invoice.servingStaff == null → claim table (set to current staff)
     * - If invoice.servingStaff == currentStaff → allow (already owned)
     * - If invoice.servingStaff != currentStaff → throw 403 Forbidden
     * 
     * @param invoice The invoice to validate
     * @throws ResponseStatusException 403 if table is claimed by another staff
     */
    private void validateAndClaimTable(Invoice invoice) {
        Employee currentEmployee = getCurrentEmployee();
        
        System.out.println("🔍 DEBUG validateAndClaimTable()");
        System.out.println("   Current employee: " + (currentEmployee != null ? currentEmployee.getFullName() + " (ID: " + currentEmployee.getId() + ")" : "NULL"));
        System.out.println("   Invoice serving staff: " + (invoice.getServingStaff() != null ? invoice.getServingStaff().getFullName() + " (ID: " + invoice.getServingStaff().getId() + ")" : "NULL"));
        
        // If table already claimed by another staff, ALWAYS BLOCK (even if currentEmployee is null)
        if (invoice.getServingStaff() != null) {
            if (currentEmployee == null) {
                // No employee context but table is claimed → block
                String claimedByStaffName = invoice.getServingStaff().getFullName();
                System.out.println("   ❌ BLOCKING: No employee context but table owned by " + claimedByStaffName);
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bàn này đang được phục vụ bởi nhân viên " + claimedByStaffName + ". " +
                    "Chỉ nhân viên đã order mới có thể tiếp tục order cho bàn này."
                );
            }
            
            if (!invoice.getServingStaff().getId().equals(currentEmployee.getId())) {
                // Table claimed by different staff → block
                String claimedByStaffName = invoice.getServingStaff().getFullName();
                System.out.println("   ❌ BLOCKING: Table owned by " + claimedByStaffName);
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bàn này đang được phục vụ bởi nhân viên " + claimedByStaffName + ". " +
                    "Chỉ nhân viên đã order mới có thể tiếp tục order cho bàn này."
                );
            }
            
            // Table already claimed by current staff → allow
            System.out.println("   ✅ Table already owned by current staff - allowing");
            return;
        }
        
        // Table not yet claimed
        if (currentEmployee == null) {
            // No employee context and table not claimed → allow but don't claim
            System.out.println("   ⚠️ No current employee - allowing operation without claiming");
            return;
        }
        
        // Claim table for current staff
        System.out.println("   ✅ Claiming table for: " + currentEmployee.getFullName());
        invoice.setServingStaff(currentEmployee);
        invoiceRepository.save(invoice);
        
        // Broadcast table claimed via WebSocket
        broadcastTableClaimed(invoice.getId(), currentEmployee.getId(), currentEmployee.getFullName());
    }
    
    /**
     * Broadcast table status update via WebSocket
     * 
     * @param invoiceId The invoice ID
     * @param staffId The staff ID who claimed the table
     * @param staffName The staff name
     */
    private void broadcastTableClaimed(Integer invoiceId, Integer staffId, String staffName) {
        try {
            java.util.Map<String, Object> message = new java.util.HashMap<>();
            message.put("invoiceId", invoiceId);
            message.put("staffId", staffId);
            message.put("staffName", staffName);
            message.put("action", "CLAIMED");
            message.put("timestamp", java.time.Instant.now().toString());
            
            messagingTemplate.convertAndSend("/topic/table-status", message);
            System.out.println("✅ Broadcasted table claimed: Invoice #" + invoiceId + " by " + staffName);
        } catch (Exception e) {
            System.err.println("❌ Failed to broadcast table status: " + e.getMessage());
        }
    }

    private InvoiceGroupResponse mapToInvoiceGroupResponse(Invoice invoice) {
        List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository
                .findByInvoiceIdWithTable(invoice.getId());
        
        List<InvoiceGroupResponse.TableInfo> tableInfos = invoiceTables.stream()
                .map(idt -> InvoiceGroupResponse.TableInfo.builder()
                        .tableId(idt.getDiningTable().getId())
                        .tableName(idt.getDiningTable().getTableName())
                        .floor(idt.getDiningTable().getFloor())
                        .area(idt.getDiningTable().getArea())
                        .build())
                .collect(Collectors.toList());
        
        String customerName = invoice.getCustomer() != null 
                ? invoice.getCustomer().getFullName() 
                : "Khách vãng lai";
        
        return InvoiceGroupResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .customerName(customerName)
                .guestCount(invoice.getGuestCount())
                .subtotalAmount(invoice.getSubtotalAmount() != null 
                        ? invoice.getSubtotalAmount() 
                        : BigDecimal.ZERO)
                .checkedInAt(invoice.getCheckedInAt())
                .tables(tableInfos)
                .servingStaffId(invoice.getServingStaff() != null ? invoice.getServingStaff().getId() : null)
                .servingStaffName(invoice.getServingStaff() != null ? invoice.getServingStaff().getFullName() : null)
                .foodNote(invoice.getFoodNote())
                .build();
    }

    public List<StaffTableResponse> getServingTables() {
        // Query all IN_PROGRESS invoices with their tables and serving staff
        List<Invoice> invoices = invoiceRepository.findByInvoiceStatus(STATUS_IN_PROGRESS);
        
        List<StaffTableResponse> responses = new ArrayList<>();
        
        for (Invoice invoice : invoices) {
            // Get tables for this invoice
            List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository
                    .findByInvoiceIdWithTable(invoice.getId());
            
            for (InvoiceDiningTable idt : invoiceTables) {
                DiningTable table = idt.getDiningTable();
                
                StaffTableResponse response = StaffTableResponse.builder()
                        .tableId(table.getId())
                        .tableName(table.getTableName())
                        .floor(table.getFloor())
                        .seatingCapacity(table.getSeatingCapacity())
                        .tableStatus(table.getTableStatus())
                        .invoiceId(invoice.getId())
                        .servingStaffId(invoice.getServingStaff() != null ? invoice.getServingStaff().getId() : null)
                        .servingStaffName(invoice.getServingStaff() != null ? invoice.getServingStaff().getFullName() : null)
                        .build();
                
                responses.add(response);
            }
        }
        
        return responses;
    }

    public StaffTableResponse getTableById(Integer tableId) {
        DiningTable table = diningTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Table not found"
                ));
        return mapToStaffTableResponse(table);
    }

    private StaffTableResponse mapToStaffTableResponse(DiningTable table) {
        return StaffTableResponse.builder()
                .tableId(table.getId())
                .tableName(table.getTableName())
                .floor(table.getFloor())
                .seatingCapacity(table.getSeatingCapacity())
                .tableStatus(table.getTableStatus())
                .build();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addItemsToInvoice(Integer invoiceId, List<OrderItemRequest> items) {
        if (invoiceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice id is required");
        }
        
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invoice not found"
                ));
        
        if (!STATUS_IN_PROGRESS.equals(invoice.getInvoiceStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invoice is not in progress");
        }
        
        // Validate and claim table (using serving_staff_id)
        validateAndClaimTable(invoice);
        
        // Also assign employee for cashier tracking
        assignAndCheckEmployee(invoice);
        
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Items is required");
        }

        // Get first table from invoice for dining table reference
        List<InvoiceDiningTable> invoiceTables = invoiceDiningTableRepository
                .findByInvoiceIdWithTable(invoiceId);
        if (invoiceTables.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invoice has no associated tables");
        }
        DiningTable diningTable = invoiceTables.get(0).getDiningTable();

        List<InvoiceItem> toSave = new ArrayList<>();
        BigDecimal addedSubtotal = BigDecimal.ZERO;

        for (OrderItemRequest item : items) {
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be >= 1");
            }

            String type = item.getItemType() == null ? "" : item.getItemType().trim().toUpperCase();
            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setInvoice(invoice);
            invoiceItem.setItemType(type);
            invoiceItem.setQuantity(item.getQuantity());
            invoiceItem.setDiningTable(diningTable);
            invoiceItem.setStatus(InvoiceItemStatus.ORDERED);

            if ("PRODUCT".equals(type)) {
                if (item.getProductId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
                }
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Product not found"));
                invoiceItem.setProduct(product);
                invoiceItem.setUnitPrice(product.getUnitPrice());

                String versionId = productVersionService.createProductSnapshot(product);
                invoiceItem.setProductVersionId(versionId);

                // DRINK items are ready-made — skip kitchen, auto-mark SERVED
                if (ProductCategory.DRINK.equals(product.getProductCategory())) {
                    invoiceItem.setStatus(InvoiceItemStatus.SERVED);
                }
                // DESSERT items are held until staff activates them
                else if (ProductCategory.DESSERT.equals(product.getProductCategory())) {
                    invoiceItem.setStatus(InvoiceItemStatus.PENDING);
                }
            } else if ("COMBO".equals(type)) {
                if (item.getProductComboId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productComboId is required");
                }
                ProductCombo combo = productComboRepository.findById(item.getProductComboId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Combo not found"));
                invoiceItem.setProductCombo(combo);
                invoiceItem.setUnitPrice(combo.getComboPrice());

                String versionId = productVersionService.createComboSnapshot(combo);
                invoiceItem.setProductVersionId(versionId);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid itemType");
            }

            if (invoiceItem.getUnitPrice() != null) {
                addedSubtotal = addedSubtotal.add(
                        invoiceItem.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                );
            }

            // DRINK (auto-SERVED): merge into existing SERVED row if any, skip creating new row
            if (InvoiceItemStatus.SERVED.equals(invoiceItem.getStatus()) && "PRODUCT".equals(type)) {
                InvoiceItem existing = invoiceItemRepository
                        .findActiveByInvoiceAndProduct(invoice.getId(), invoiceItem.getProduct().getId())
                        .orElse(null);
                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + invoiceItem.getQuantity());
                    invoiceItemRepository.save(existing);
                    continue; // merged — subtotal already counted, no new row needed
                }
            }

            toSave.add(invoiceItem);
        }

        invoiceItemRepository.saveAll(toSave);

        BigDecimal currentSubtotal = invoice.getSubtotalAmount() == null
                ? BigDecimal.ZERO
                : invoice.getSubtotalAmount();
        invoice.setSubtotalAmount(currentSubtotal.add(addedSubtotal));
        invoiceRepository.save(invoice);

        // Only broadcast to kitchen for items that need cooking (status ORDERED)
        long kitchenCount = toSave.stream()
                .filter(i -> InvoiceItemStatus.ORDERED.equals(i.getStatus()))
                .count();
        if (kitchenCount > 0) {
            kitchenBroadcastService.broadcastBulkKitchenUpdate("ITEMS_ORDERED", (int) kitchenCount);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addItemsToTable(Integer tableId, List<OrderItemRequest> items) {
        if (tableId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table id is required");
        }
        DiningTable diningTable = diningTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Dining table not found"
                ));
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Items is required");
        }

        Invoice invoice = getSingleInProgressInvoice(tableId);
        
        // Validate and claim table (using serving_staff_id)
        validateAndClaimTable(invoice);
        
        // Also assign employee for cashier tracking
        assignAndCheckEmployee(invoice);

        List<InvoiceItem> toSave = new ArrayList<>();
        BigDecimal addedSubtotal = BigDecimal.ZERO;

        for (OrderItemRequest item : items) {
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be >= 1");
            }

            String type = item.getItemType() == null ? "" : item.getItemType().trim().toUpperCase();
            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setInvoice(invoice);
            invoiceItem.setItemType(type);
            invoiceItem.setQuantity(item.getQuantity());
            invoiceItem.setDiningTable(diningTable);
            invoiceItem.setStatus(InvoiceItemStatus.ORDERED);

            if ("PRODUCT".equals(type)) {
                if (item.getProductId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
                }
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Product not found"));
                invoiceItem.setProduct(product);
                invoiceItem.setUnitPrice(product.getUnitPrice());

                String versionId = productVersionService.createProductSnapshot(product);
                invoiceItem.setProductVersionId(versionId);

                // DRINK items are ready-made — skip kitchen, auto-mark SERVED
                if (ProductCategory.DRINK.equals(product.getProductCategory())) {
                    invoiceItem.setStatus(InvoiceItemStatus.SERVED);
                }
                // DESSERT items are held until staff activates them
                else if (ProductCategory.DESSERT.equals(product.getProductCategory())) {
                    invoiceItem.setStatus(InvoiceItemStatus.PENDING);
                }
            } else if ("COMBO".equals(type)) {
                if (item.getProductComboId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productComboId is required");
                }
                ProductCombo combo = productComboRepository.findById(item.getProductComboId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Combo not found"));
                invoiceItem.setProductCombo(combo);
                invoiceItem.setUnitPrice(combo.getComboPrice());

                String versionId = productVersionService.createComboSnapshot(combo);
                invoiceItem.setProductVersionId(versionId);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid itemType");
            }

            if (invoiceItem.getUnitPrice() != null) {
                addedSubtotal = addedSubtotal.add(
                        invoiceItem.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                );
            }

            // DRINK (auto-SERVED): merge into existing SERVED row if any, skip creating new row
            if (InvoiceItemStatus.SERVED.equals(invoiceItem.getStatus()) && "PRODUCT".equals(type)) {
                InvoiceItem existing = invoiceItemRepository
                        .findActiveByInvoiceAndProduct(invoice.getId(), invoiceItem.getProduct().getId())
                        .orElse(null);
                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + invoiceItem.getQuantity());
                    invoiceItemRepository.save(existing);
                    continue; // merged — subtotal already counted, no new row needed
                }
            }

            toSave.add(invoiceItem);
        }

        invoiceItemRepository.saveAll(toSave);

        BigDecimal currentSubtotal = invoice.getSubtotalAmount() == null
                ? BigDecimal.ZERO
                : invoice.getSubtotalAmount();
        invoice.setSubtotalAmount(currentSubtotal.add(addedSubtotal));
        invoiceRepository.save(invoice);

        // Only broadcast to kitchen for items that need cooking (status ORDERED)
        long kitchenCount = toSave.stream()
                .filter(i -> InvoiceItemStatus.ORDERED.equals(i.getStatus()))
                .count();
        if (kitchenCount > 0) {
            kitchenBroadcastService.broadcastBulkKitchenUpdate("ITEMS_ORDERED", (int) kitchenCount);
        }
    }

    /**
     * Safely get single IN_PROGRESS invoice for a table.
     * Throws CONFLICT error if multiple invoices found (merged table issue).
     * Throws NOT_FOUND if no invoice found.
     */
    private Invoice getSingleInProgressInvoice(Integer tableId) {
        if (tableId == null || tableId < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table id is invalid");
        }

        List<Invoice> invoices = invoiceDiningTableRepository.findDistinctInvoicesByTableAndStatuses(
                tableId,
                List.of(STATUS_IN_PROGRESS)
        );

        if (invoices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "No active invoice for table #" + tableId);
        }
        
        if (invoices.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Table #" + tableId + " is attached to multiple active invoices. Please resolve the conflict.");
        }
        
        return invoices.get(0);
    }

    public List<InvoiceItemResponse> getInvoiceItems(Integer invoiceId) {
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithItem(invoiceId);
        
        return items.stream()
                .filter(item -> item.getStatus() != InvoiceItemStatus.CANCELLED)
                .map(item -> {
                    InvoiceItemResponse response = new InvoiceItemResponse();
                    response.setId(item.getId());
                    
                    String itemName = null;
                    if (item.getProductVersionId() != null) {
                        com.example.datn_sd_29.product_version.document.ProductVersionDocument version = 
                                productVersionService.getVersionById(item.getProductVersionId());
                        if (version != null) {
                            itemName = version.getItemName();
                        }
                    }
                    
                    if (itemName == null) {
                        if (item.getProduct() != null) {
                            itemName = item.getProduct().getProductName();
                        } else if (item.getProductCombo() != null) {
                            itemName = item.getProductCombo().getComboName();
                        }
                    }
                    
                    response.setItemName(itemName);
                    response.setQuantity(item.getQuantity());
                    response.setPrice(item.getUnitPrice());
                    response.setStatus(item.getStatus().name());
                    
                    return response;
                })
                .toList();
    }
}
