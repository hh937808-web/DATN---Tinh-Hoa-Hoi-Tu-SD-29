package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.common.service.TableStatusBroadcastService;
import com.example.datn_sd_29.common.service.InvoiceBroadcastService;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.dining_table.entity.DiningTable;
import com.example.datn_sd_29.dining_table.repository.DiningTableRepository;
import com.example.datn_sd_29.invoice.dto.PaymentCheckoutRequest;
import com.example.datn_sd_29.invoice.dto.PaymentCheckoutResponse;
import com.example.datn_sd_29.invoice.dto.PaymentDetailResponse;
import com.example.datn_sd_29.invoice.dto.PaymentItemResponse;
import com.example.datn_sd_29.invoice.dto.PaymentVoucherResponse;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoiceItemStatus;
import com.example.datn_sd_29.invoice.entity.InvoicePayment;
import com.example.datn_sd_29.invoice.entity.InvoiceVoucher;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
import com.example.datn_sd_29.invoice.repository.InvoicePaymentRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceVoucherRepository;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.voucher.entity.CustomerVoucher;
import com.example.datn_sd_29.voucher.entity.ProductVoucher;
import com.example.datn_sd_29.voucher.entity.ProductComboVoucher;
import com.example.datn_sd_29.voucher.enums.VoucherStatus;
import com.example.datn_sd_29.voucher.repository.CustomerVoucherRepository;
import com.example.datn_sd_29.voucher.repository.ProductVoucherRepository;
import com.example.datn_sd_29.voucher.repository.ProductComboVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final BigDecimal POINT_VALUE = BigDecimal.valueOf(1000);
    private static final BigDecimal EARN_POINT_RATE = BigDecimal.valueOf(0.02); // 2% cashback

    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final InvoiceVoucherRepository invoiceVoucherRepository;
    private final CustomerRepository customerRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final ProductVoucherRepository productVoucherRepository;
    private final ProductComboVoucherRepository productComboVoucherRepository;
    private final DiningTableRepository diningTableRepository;
    private final TableStatusBroadcastService tableStatusBroadcastService;
    private final InvoiceBroadcastService invoiceBroadcastService;
    
    @Value("${payment.vat.percent:10}")
    private BigDecimal vatPercent;
    
    @Value("${payment.service-fee.percent:5}")
    private BigDecimal serviceFeePercent;
    
    @Value("${payment.points.max-usage-percent:40}")
    private BigDecimal maxPointsUsagePercent;

    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentByTable(Integer tableId) {
        Invoice invoice = getSingleInProgressInvoice(tableId);

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithItem(invoice.getId()).stream()
                .filter(item -> item.getStatus() != InvoiceItemStatus.CANCELLED)
                .toList();
        List<InvoiceDiningTable> tableLinks =
                invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());

        PaymentDetailResponse response = new PaymentDetailResponse();
        response.setInvoiceId(invoice.getId());
        response.setInvoiceCode(invoice.getInvoiceCode());
        response.setInvoiceStatus(invoice.getInvoiceStatus());
        response.setInvoiceChannel(invoice.getInvoiceChannel());
        response.setReservedAt(invoice.getReservedAt());
        response.setCheckedInAt(invoice.getCheckedInAt());
        response.setGuestCount(invoice.getGuestCount());

        Customer customer = invoice.getCustomer();
        if (customer == null) {
            response.setCustomerType("GUEST");
            // Use guestName if available, otherwise default to "Khách lẻ"
            if (invoice.getGuestName() != null && !invoice.getGuestName().trim().isEmpty()) {
                response.setCustomerName(invoice.getGuestName());
            } else {
                response.setCustomerName("Khách lẻ");
            }
            response.setCustomerPhone("");
            response.setLoyaltyPoints(0);
        } else {
            response.setCustomerType("MEMBER");
            response.setCustomerName(customer.getFullName());
            response.setCustomerPhone(customer.getPhoneNumber());
            response.setLoyaltyPoints(customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints());
        }

        if (invoice.getEmployee() != null) {
            response.setStaffName(invoice.getEmployee().getFullName());
        } else {
            response.setStaffName("Chưa phân công");
        }

        List<PaymentDetailResponse.TableSummary> tables = new ArrayList<>();
        for (InvoiceDiningTable link : tableLinks) {
            PaymentDetailResponse.TableSummary t = new PaymentDetailResponse.TableSummary();
            t.setId(link.getDiningTable().getId());
            t.setTableName(link.getDiningTable().getTableName());
            t.setSeatingCapacity(link.getDiningTable().getSeatingCapacity());
            tables.add(t);
        }
        response.setTables(tables);

        List<PaymentItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (InvoiceItem item : items) {
            PaymentItemResponse i = new PaymentItemResponse();
            i.setId(item.getId());
            i.setQuantity(item.getQuantity());
            i.setUnitPrice(item.getUnitPrice());
            i.setDiscount(BigDecimal.ZERO);
            i.setStatus(item.getStatus() != null ? item.getStatus().name() : null); // Set status
            if (item.getProduct() != null) {
                i.setName(item.getProduct().getProductName());
                i.setType("PRODUCT");
                i.setProductId(item.getProduct().getId());
            } else if (item.getProductCombo() != null) {
                i.setName(item.getProductCombo().getComboName());
                i.setType("COMBO");
                i.setComboId(item.getProductCombo().getId());
            } else {
                i.setName("Unknown");
                i.setType(item.getItemType());
            }
            BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .subtract(i.getDiscount());
            i.setLineTotal(lineTotal);
            subtotal = subtotal.add(lineTotal);
            itemResponses.add(i);
        }

        // Auto-apply product vouchers
        autoApplyProductVouchers(items, itemResponses, customer);
        
        // Recalculate subtotal and item voucher discount after auto-apply
        // CRITICAL FIX: subtotal must be BEFORE discount (same as checkout method)
        subtotal = BigDecimal.ZERO;
        BigDecimal itemVoucherDiscount = BigDecimal.ZERO;
        for (PaymentItemResponse i : itemResponses) {
            // Calculate subtotal BEFORE discount
            BigDecimal lineSubtotal = i.getUnitPrice()
                    .multiply(BigDecimal.valueOf(i.getQuantity()));
            subtotal = subtotal.add(lineSubtotal);
            
            // Calculate lineTotal AFTER discount (for display only)
            BigDecimal lineTotal = lineSubtotal.subtract(i.getDiscount());
            i.setLineTotal(lineTotal);
            
            // Sum up item voucher discounts
            itemVoucherDiscount = itemVoucherDiscount.add(i.getDiscount());
        }

        response.setItems(itemResponses);
        response.setSubtotal(subtotal);
        response.setItemVoucherDiscount(itemVoucherDiscount);
        response.setVatPercent(vatPercent);
        response.setServiceFeePercent(serviceFeePercent);
        response.setPointValue(POINT_VALUE.intValue());

        // Auto-apply invoice voucher (CustomerVoucher) if available
        BigDecimal subtotalAfterItemVoucher = subtotal.subtract(itemVoucherDiscount);
        CustomerVoucher autoVoucher = autoApplyInvoiceVoucher(customer, subtotalAfterItemVoucher);
        if (autoVoucher != null) {
            response.setAutoAppliedVoucherId(autoVoucher.getId());
            response.setAutoAppliedVoucherCode(autoVoucher.getPersonalVoucher().getVoucherCode());
            response.setAutoAppliedVoucherName(autoVoucher.getPersonalVoucher().getVoucherName());
            response.setAutoAppliedVoucherPercent(autoVoucher.getPersonalVoucher().getDiscountPercent());
            
            // Calculate discount
            Integer percent = autoVoucher.getPersonalVoucher().getDiscountPercent();
            if (percent != null && percent > 0) {
                BigDecimal discount = subtotalAfterItemVoucher.multiply(BigDecimal.valueOf(percent))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                response.setAutoAppliedVoucherDiscount(discount);
            } else {
                response.setAutoAppliedVoucherDiscount(BigDecimal.ZERO);
            }
        }
        
        // Calculate totalPayable (with points = 0 for initial display)
        // Frontend will recalculate when user changes points
        BigDecimal baseAfterVoucher = subtotal.subtract(itemVoucherDiscount);
        if (autoVoucher != null && response.getAutoAppliedVoucherDiscount() != null) {
            baseAfterVoucher = baseAfterVoucher.subtract(response.getAutoAppliedVoucherDiscount());
        }
        baseAfterVoucher = baseAfterVoucher.max(BigDecimal.ZERO);
        
        // Calculate tax on baseAfterVoucher (points = 0 initially)
        BigDecimal taxAmount = baseAfterVoucher.multiply(vatPercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        BigDecimal serviceFeeAmount = baseAfterVoucher.multiply(serviceFeePercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        
        BigDecimal totalPayable = baseAfterVoucher.add(taxAmount).add(serviceFeeAmount);
        response.setTotalPayable(totalPayable);

        response.setVouchers(loadAvailableVouchers(customer));
        return response;
    }

    @Transactional(readOnly = true)
    public List<PaymentDetailResponse> getAllInProgressInvoices() {
        List<Invoice> invoices = invoiceRepository.findByInvoiceStatus(STATUS_IN_PROGRESS);
        List<PaymentDetailResponse> responses = new ArrayList<>();

        for (Invoice invoice : invoices) {
            List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithItem(invoice.getId()).stream()
                    .filter(item -> item.getStatus() != InvoiceItemStatus.CANCELLED)
                    .toList();
            List<InvoiceDiningTable> tableLinks =
                    invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());

            PaymentDetailResponse response = new PaymentDetailResponse();
            response.setInvoiceId(invoice.getId());
            response.setInvoiceCode(invoice.getInvoiceCode());
            response.setInvoiceStatus(invoice.getInvoiceStatus());
            response.setInvoiceChannel(invoice.getInvoiceChannel());
            response.setReservedAt(invoice.getReservedAt());
            response.setCheckedInAt(invoice.getCheckedInAt());
            response.setGuestCount(invoice.getGuestCount());

            Customer customer = invoice.getCustomer();
            if (customer == null) {
                response.setCustomerType("GUEST");
                // Use guestName if available, otherwise default to "Khách lẻ"
                if (invoice.getGuestName() != null && !invoice.getGuestName().trim().isEmpty()) {
                    response.setCustomerName(invoice.getGuestName());
                } else {
                    response.setCustomerName("Khách lẻ");
                }
                response.setCustomerPhone("");
                response.setLoyaltyPoints(0);
            } else {
                response.setCustomerType("MEMBER");
                response.setCustomerName(customer.getFullName());
                response.setCustomerPhone(customer.getPhoneNumber());
                response.setLoyaltyPoints(customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints());
            }

            if (invoice.getEmployee() != null) {
                response.setStaffName(invoice.getEmployee().getFullName());
            } else {
                response.setStaffName("Chưa phân công");
            }

            List<PaymentDetailResponse.TableSummary> tables = new ArrayList<>();
            for (InvoiceDiningTable link : tableLinks) {
                PaymentDetailResponse.TableSummary t = new PaymentDetailResponse.TableSummary();
                t.setId(link.getDiningTable().getId());
                t.setTableName(link.getDiningTable().getTableName());
                t.setSeatingCapacity(link.getDiningTable().getSeatingCapacity());
                tables.add(t);
            }
            response.setTables(tables);

            List<PaymentItemResponse> itemResponses = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;
            for (InvoiceItem item : items) {
                PaymentItemResponse i = new PaymentItemResponse();
                i.setId(item.getId());
                i.setQuantity(item.getQuantity());
                i.setUnitPrice(item.getUnitPrice());
                i.setDiscount(BigDecimal.ZERO);
                i.setStatus(item.getStatus() != null ? item.getStatus().name() : null); // Set status
                if (item.getProduct() != null) {
                    i.setName(item.getProduct().getProductName());
                    i.setType("PRODUCT");
                    i.setProductId(item.getProduct().getId());
                } else if (item.getProductCombo() != null) {
                    i.setName(item.getProductCombo().getComboName());
                    i.setType("COMBO");
                    i.setComboId(item.getProductCombo().getId());
                } else {
                    i.setName("Unknown");
                    i.setType(item.getItemType());
                }
                BigDecimal lineTotal = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))
                        .subtract(i.getDiscount());
                i.setLineTotal(lineTotal);
                subtotal = subtotal.add(lineTotal);
                itemResponses.add(i);
            }

            // Auto-apply product vouchers
            autoApplyProductVouchers(items, itemResponses, customer);
            
            // Recalculate subtotal and item voucher discount after auto-apply
            // CRITICAL FIX: subtotal must be BEFORE discount (same as checkout method)
            subtotal = BigDecimal.ZERO;
            BigDecimal itemVoucherDiscount = BigDecimal.ZERO;
            for (PaymentItemResponse i : itemResponses) {
                // Calculate subtotal BEFORE discount
                BigDecimal lineSubtotal = i.getUnitPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity()));
                subtotal = subtotal.add(lineSubtotal);
                
                // Calculate lineTotal AFTER discount (for display only)
                BigDecimal lineTotal = lineSubtotal.subtract(i.getDiscount());
                i.setLineTotal(lineTotal);
                
                // Sum up item voucher discounts
                itemVoucherDiscount = itemVoucherDiscount.add(i.getDiscount());
            }

            response.setItems(itemResponses);
            response.setSubtotal(subtotal);
            response.setItemVoucherDiscount(itemVoucherDiscount);
            response.setVatPercent(vatPercent);
            response.setServiceFeePercent(serviceFeePercent);
            response.setPointValue(POINT_VALUE.intValue());
            
            // Auto-apply invoice voucher (CustomerVoucher) if available
            BigDecimal subtotalAfterItemVoucher = subtotal.subtract(itemVoucherDiscount);
            CustomerVoucher autoVoucher = autoApplyInvoiceVoucher(customer, subtotalAfterItemVoucher);
            if (autoVoucher != null) {
                response.setAutoAppliedVoucherId(autoVoucher.getId());
                response.setAutoAppliedVoucherCode(autoVoucher.getPersonalVoucher().getVoucherCode());
                response.setAutoAppliedVoucherName(autoVoucher.getPersonalVoucher().getVoucherName());
                response.setAutoAppliedVoucherPercent(autoVoucher.getPersonalVoucher().getDiscountPercent());
                
                // Calculate discount
                Integer percent = autoVoucher.getPersonalVoucher().getDiscountPercent();
                if (percent != null && percent > 0) {
                    BigDecimal discount = subtotalAfterItemVoucher.multiply(BigDecimal.valueOf(percent))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                    response.setAutoAppliedVoucherDiscount(discount);
                } else {
                    response.setAutoAppliedVoucherDiscount(BigDecimal.ZERO);
                }
            }
            
            // Calculate totalPayable (with points = 0 for initial display)
            BigDecimal baseAfterVoucher = subtotal.subtract(itemVoucherDiscount);
            if (autoVoucher != null && response.getAutoAppliedVoucherDiscount() != null) {
                baseAfterVoucher = baseAfterVoucher.subtract(response.getAutoAppliedVoucherDiscount());
            }
            baseAfterVoucher = baseAfterVoucher.max(BigDecimal.ZERO);
            
            // Calculate tax on baseAfterVoucher (points = 0 initially)
            BigDecimal taxAmount = baseAfterVoucher.multiply(vatPercent)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
            BigDecimal serviceFeeAmount = baseAfterVoucher.multiply(serviceFeePercent)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
            
            BigDecimal totalPayable = baseAfterVoucher.add(taxAmount).add(serviceFeeAmount);
            response.setTotalPayable(totalPayable);
            
            response.setVouchers(loadAvailableVouchers(customer));

            responses.add(response);
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<String> debugAllInvoices() {
        List<Invoice> allInvoices = invoiceRepository.findAll();
        List<String> debug = new ArrayList<>();
        debug.add("Total invoices: " + allInvoices.size());
        
        for (Invoice inv : allInvoices) {
            List<InvoiceDiningTable> tables = invoiceDiningTableRepository.findByInvoiceIdWithTable(inv.getId());
            String tableInfo = tables.stream()
                .map(t -> "Table#" + t.getDiningTable().getId() + "(" + t.getDiningTable().getTableName() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("No tables");
            
            debug.add(String.format("Invoice ID=%d, Code=%s, Status=%s, Tables=[%s]", 
                inv.getId(), inv.getInvoiceCode(), inv.getInvoiceStatus(), tableInfo));
        }
        
        return debug;
    }

    @Transactional
    public PaymentCheckoutResponse checkout(PaymentCheckoutRequest request) {
        Invoice invoice = getSingleInProgressInvoice(request.getTableId());

        if (STATUS_PAID.equals(invoice.getInvoiceStatus()) || STATUS_CANCELLED.equals(invoice.getInvoiceStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is closed");
        }

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithItem(invoice.getId()).stream()
                .filter(item -> item.getStatus() != InvoiceItemStatus.CANCELLED)
                .toList();
        
        // RULE: Block payment if invoice has no items
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Cannot process payment: Invoice has no items");
        }
        
        // RULE: Block payment if any item is not SERVED yet
        List<InvoiceItem> unservedItems = items.stream()
                .filter(item -> item.getStatus() != InvoiceItemStatus.SERVED)
                .toList();
        
        if (!unservedItems.isEmpty()) {
            String unservedItemNames = unservedItems.stream()
                    .map(item -> {
                        if (item.getProduct() != null) {
                            return item.getProduct().getProductName();
                        } else if (item.getProductCombo() != null) {
                            return item.getProductCombo().getComboName();
                        }
                        return "Unknown";
                    })
                    .collect(Collectors.joining(", "));
            
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Cannot process payment: Some items have not been served yet: " + unservedItemNames);
        }
        
        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Customer customer = invoice.getCustomer();
        
        // CRITICAL FIX: Auto-apply product vouchers BEFORE calculating discount
        // This ensures discount is saved to database for consistency
        List<PaymentItemResponse> tempItemResponses = new ArrayList<>();
        for (InvoiceItem item : items) {
            PaymentItemResponse temp = new PaymentItemResponse();
            temp.setId(item.getId());
            temp.setQuantity(item.getQuantity());
            temp.setUnitPrice(item.getUnitPrice());
            tempItemResponses.add(temp);
        }
        autoApplyProductVouchers(items, tempItemResponses, customer);
        
        // Calculate item voucher discount from items that now have vouchers applied
        BigDecimal itemVoucherDiscount = items.stream()
                .map(i -> i.getAppliedVoucherDiscount() != null ? i.getAppliedVoucherDiscount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Validate Product/Combo vouchers if explicitly provided in request
        // (Item vouchers are already auto-applied in getPaymentByTable, we just validate here)
        List<ProductVoucher> appliedProductVouchers = new ArrayList<>();
        List<ProductComboVoucher> appliedComboVouchers = new ArrayList<>();
        
        if (request.getVoucherCodes() != null && !request.getVoucherCodes().isEmpty()) {
            LocalDate today = LocalDate.now();
            
            for (String voucherCode : request.getVoucherCodes()) {
                // Try to find as ProductVoucher first
                ProductVoucher productVoucher = productVoucherRepository.findByVoucherCode(voucherCode).orElse(null);
                
                if (productVoucher != null) {
                    // Validate ProductVoucher
                    if (!Boolean.TRUE.equals(productVoucher.getIsActive())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Voucher " + voucherCode + " is not active");
                    }
                    
                    if (productVoucher.getRemainingQuantity() == null || productVoucher.getRemainingQuantity() <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Voucher " + voucherCode + " has no remaining uses");
                    }
                    
                    if (productVoucher.getValidFrom() != null && today.isBefore(productVoucher.getValidFrom())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Voucher " + voucherCode + " is not yet valid");
                    }
                    
                    if (productVoucher.getValidTo() != null && today.isAfter(productVoucher.getValidTo())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Voucher " + voucherCode + " has expired");
                    }
                    
                    // Check if invoice has matching product
                    boolean hasMatchingProduct = items.stream()
                        .anyMatch(item -> item.getProduct() != null && 
                                         item.getProduct().getId().equals(productVoucher.getProduct().getId()));
                    
                    if (!hasMatchingProduct) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Invoice does not contain product for voucher " + voucherCode);
                    }
                    
                    appliedProductVouchers.add(productVoucher);
                    continue;
                }
                
                // Try to find as ProductComboVoucher
                ProductComboVoucher comboVoucher = productComboVoucherRepository.findByVoucherCode(voucherCode).orElse(null);
                
                if (comboVoucher != null) {
                    // Validate ComboVoucher
                    if (!Boolean.TRUE.equals(comboVoucher.getIsActive())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Voucher " + voucherCode + " is not active");
                    }
                    
                    if (comboVoucher.getRemainingQuantity() == null || comboVoucher.getRemainingQuantity() <= 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Voucher " + voucherCode + " has no remaining uses");
                    }
                    
                    if (comboVoucher.getValidFrom() != null && today.isBefore(comboVoucher.getValidFrom())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Voucher " + voucherCode + " is not yet valid");
                    }
                    
                    if (comboVoucher.getValidTo() != null && today.isAfter(comboVoucher.getValidTo())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Voucher " + voucherCode + " has expired");
                    }
                    
                    // Check if invoice has matching combo
                    boolean hasMatchingCombo = items.stream()
                        .anyMatch(item -> item.getProductCombo() != null && 
                                         item.getProductCombo().getId().equals(comboVoucher.getProductCombo().getId()));
                    
                    if (!hasMatchingCombo) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Invoice does not contain combo for voucher " + voucherCode);
                    }
                    
                    appliedComboVouchers.add(comboVoucher);
                    continue;
                }
                
                // Voucher not found
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Voucher code " + voucherCode + " not found");
            }
        }

        BigDecimal invoiceVoucherDiscount = BigDecimal.ZERO;
        CustomerVoucher customerVoucher = null;
        
        // If user explicitly selected a voucher, use it
        if (request.getCustomerVoucherId() != null) {
            // Try to find voucher - either customer-specific or public (customer_id = NULL)
            if (customer != null) {
                // Try customer-specific voucher first
                customerVoucher = customerVoucherRepository
                        .findByIdAndCustomerId(request.getCustomerVoucherId(), customer.getId())
                        .orElse(null);
            }
            
            // If not found, try public voucher (customer_id = NULL)
            if (customerVoucher == null) {
                customerVoucher = customerVoucherRepository
                        .findById(request.getCustomerVoucherId())
                        .filter(cv -> cv.getCustomer() == null) // Only public vouchers
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));
            }
        } else {
            // Auto-apply best available voucher if user didn't select one
            BigDecimal subtotalAfterItemVoucher = subtotal.subtract(itemVoucherDiscount);
            customerVoucher = autoApplyInvoiceVoucher(customer, subtotalAfterItemVoucher);
        }
        
        // Validate and calculate discount if voucher is selected/auto-applied
        if (customerVoucher != null) {
            if (!VoucherStatus.HOAT_DONG.equals(customerVoucher.getVoucherStatus())
                    || customerVoucher.getRemainingQuantity() == null
                    || customerVoucher.getRemainingQuantity() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is not available");
            }

            if (customerVoucher.getExpiresAt() != null
                    && customerVoucher.getExpiresAt().isBefore(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher expired");
            }

            // Validate minimum order amount
            BigDecimal baseAfterItemVoucher = subtotal.subtract(itemVoucherDiscount);
            BigDecimal minOrderAmount = customerVoucher.getPersonalVoucher().getMinOrderAmount();
            if (minOrderAmount != null && baseAfterItemVoucher.compareTo(minOrderAmount) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Minimum order amount not met. Required: " + minOrderAmount + ", Current: " + baseAfterItemVoucher);
            }

            Integer percent = customerVoucher.getPersonalVoucher().getDiscountPercent();
            if (percent != null && percent > 0) {
                BigDecimal base = subtotal.subtract(itemVoucherDiscount);
                invoiceVoucherDiscount = base.multiply(BigDecimal.valueOf(percent))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
            }
        }

        BigDecimal baseAfterVoucher = subtotal.subtract(itemVoucherDiscount).subtract(invoiceVoucherDiscount);
        if (baseAfterVoucher.compareTo(BigDecimal.ZERO) < 0) {
            baseAfterVoucher = BigDecimal.ZERO;
        }

        // RULE: Limit points usage to max percentage of invoice (30-50%)
        int usePoints = request.getUsePoints() == null ? 0 : request.getUsePoints();
        int customerPoints = customer == null || customer.getLoyaltyPoints() == null
                ? 0
                : customer.getLoyaltyPoints();
        
        if (usePoints > customerPoints) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Not enough points. Customer has " + customerPoints + " points");
        }
        
        // Calculate max points allowed based on invoice amount
        BigDecimal maxPointsValue = baseAfterVoucher
                .multiply(maxPointsUsagePercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        int maxPointsAllowed = maxPointsValue.divide(POINT_VALUE, 0, RoundingMode.FLOOR).intValue();
        
        if (usePoints > maxPointsAllowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Points usage exceeds limit. Maximum " + maxPointsAllowed + " points (" + 
                maxPointsUsagePercent.intValue() + "% of invoice) can be used");
        }

        // CRITICAL FIX: Calculate VAT and service fee on baseAfterVoucher (BEFORE points deduction)
        // Points are a payment method, not a discount, so tax should be calculated before points
        BigDecimal taxAmount = BigDecimal.ZERO;
        if (vatPercent.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = baseAfterVoucher.multiply(vatPercent)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        }

        BigDecimal serviceFeeAmount = BigDecimal.ZERO;
        if (serviceFeePercent.compareTo(BigDecimal.ZERO) > 0) {
            serviceFeeAmount = baseAfterVoucher.multiply(serviceFeePercent)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        }

        // Calculate totalPayable BEFORE points deduction
        BigDecimal totalPayableBeforePoints = baseAfterVoucher.add(taxAmount).add(serviceFeeAmount);

        // Now apply points discount (points reduce the amount customer needs to pay)
        BigDecimal pointsDiscount = POINT_VALUE.multiply(BigDecimal.valueOf(usePoints));
        if (pointsDiscount.compareTo(totalPayableBeforePoints) > 0) {
            pointsDiscount = totalPayableBeforePoints.max(BigDecimal.ZERO);
        }

        // Final amount customer needs to pay (after points)
        BigDecimal totalPayable = totalPayableBeforePoints.subtract(pointsDiscount);
        if (totalPayable.compareTo(BigDecimal.ZERO) < 0) {
            totalPayable = BigDecimal.ZERO;
        }

        BigDecimal totalDiscount = itemVoucherDiscount
                .add(invoiceVoucherDiscount)
                .add(pointsDiscount);

        List<PaymentCheckoutRequest.PaymentLine> paymentLines = request.getPayments();
        BigDecimal cashReceived = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        String finalMethod = request.getPaymentMethod();

        if (paymentLines != null && !paymentLines.isEmpty()) {
            for (PaymentCheckoutRequest.PaymentLine line : paymentLines) {
                if (line.getAmount() == null || line.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment amount");
                }
                if (line.getMethod() == null || line.getMethod().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment method is required");
                }
                totalPaid = totalPaid.add(line.getAmount());
                if ("CASH".equalsIgnoreCase(line.getMethod())) {
                    cashReceived = cashReceived.add(line.getAmount());
                }
            }
            if (totalPaid.compareTo(totalPayable) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Total paid is not enough. Required: " + totalPayable + ", Paid: " + totalPaid);
            }
            if (paymentLines.size() == 1) {
                finalMethod = paymentLines.get(0).getMethod();
            } else {
                finalMethod = "MIXED";
            }
        } else {
            if (finalMethod == null || finalMethod.isBlank()) {
                finalMethod = "CASH";
            }
            if ("CASH".equalsIgnoreCase(finalMethod)) {
                cashReceived = request.getCashReceived() == null
                        ? BigDecimal.ZERO
                        : request.getCashReceived();
                if (cashReceived.compareTo(totalPayable) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cash is not enough");
                }
                totalPaid = cashReceived;
            } else {
                totalPaid = totalPayable;
            }
        }

        invoice.setSubtotalAmount(subtotal);
        invoice.setDiscountAmount(totalDiscount);
        // Keep manual discount fields as NULL (removed from business logic but kept for audit)
        invoice.setManualDiscountPercent(null);
        invoice.setManualDiscountAmount(null);
        invoice.setTaxPercent(vatPercent);
        invoice.setTaxAmount(taxAmount);
        invoice.setServiceFeePercent(serviceFeePercent);
        invoice.setServiceFeeAmount(serviceFeeAmount);
        invoice.setPaymentMethod(finalMethod);
        invoice.setPaidAt(Instant.now());
        invoice.setInvoiceStatus(STATUS_PAID);
        invoice.setUsedPoints(usePoints);

        // Earn points: 2% of total payable (floor rounding)
        int earnedPoints = totalPayable.multiply(EARN_POINT_RATE).divide(POINT_VALUE, 0, RoundingMode.FLOOR).intValue();
        invoice.setEarnedPoints(earnedPoints);

        invoiceRepository.save(invoice);

        // Clear table status to AVAILABLE after payment (Requirements 4.1, 4.3, 4.4)
        List<InvoiceDiningTable> tableLinks = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
        List<Integer> tableIds = tableLinks.stream()
                .map(link -> link.getDiningTable().getId())
                .toList();
        
        if (!tableIds.isEmpty()) {
            diningTableRepository.updateTableStatusByIdIn(tableIds, "AVAILABLE");
            
            // Broadcast table status change via WebSocket (Requirement 4.1)
            tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "AVAILABLE");
        }

        // Broadcast invoice update
        invoiceBroadcastService.broadcastInvoiceUpdate(
            invoice.getId(),
            invoice.getInvoiceCode(),
            "PAID"
        );

        if (paymentLines != null && !paymentLines.isEmpty()) {
            for (PaymentCheckoutRequest.PaymentLine line : paymentLines) {
                InvoicePayment invoicePayment = new InvoicePayment();
                invoicePayment.setInvoice(invoice);
                invoicePayment.setPaymentMethod(line.getMethod());
                invoicePayment.setAmount(line.getAmount());
                invoicePayment.setNote(line.getNote());
                invoicePayment.setCreatedAt(Instant.now());
                invoicePaymentRepository.save(invoicePayment);
            }
        } else {
            InvoicePayment invoicePayment = new InvoicePayment();
            invoicePayment.setInvoice(invoice);
            invoicePayment.setPaymentMethod(finalMethod);
            invoicePayment.setAmount(totalPaid);
            invoicePayment.setCreatedAt(Instant.now());
            invoicePaymentRepository.save(invoicePayment);
        }

        if (customer != null) {
            customer.setLoyaltyPoints(customerPoints - usePoints + earnedPoints);
            customerRepository.save(customer);
        }

        if (customerVoucher != null) {
            // Decrease remaining uses
            int remain = customerVoucher.getRemainingQuantity() == null ? 0 : customerVoucher.getRemainingQuantity();
            customerVoucher.setRemainingQuantity(Math.max(0, remain - 1));
            
            // Update status based on remaining uses
            if (customerVoucher.getRemainingQuantity() <= 0) {
                customerVoucher.setVoucherStatus(VoucherStatus.DA_DUNG);
            }
            // If remaining > 0, keep status as HOAT_DONG (don't change it)
            
            customerVoucherRepository.save(customerVoucher);

            InvoiceVoucher invoiceVoucher = new InvoiceVoucher();
            invoiceVoucher.setInvoice(invoice);
            invoiceVoucher.setVoucherScope("CUSTOMER");
            invoiceVoucher.setCustomerVoucher(customerVoucher);
            invoiceVoucherRepository.save(invoiceVoucher);
        }
        
        // Decrement remaining quantity and save Product vouchers
        for (ProductVoucher pv : appliedProductVouchers) {
            pv.setRemainingQuantity(pv.getRemainingQuantity() - 1);
            
            // FIX #11: Auto-update is_active when remaining_quantity = 0
            if (pv.getRemainingQuantity() <= 0) {
                pv.setIsActive(false);
            }
            
            productVoucherRepository.save(pv);
            
            // Save voucher info to InvoiceItem
            InvoiceItem matchingItem = items.stream()
                .filter(item -> item.getProduct() != null && 
                               item.getProduct().getId().equals(pv.getProduct().getId()))
                .findFirst()
                .orElse(null);
            
            if (matchingItem != null && pv.getDiscountPercent() != null && pv.getDiscountPercent() > 0) {
                BigDecimal itemTotal = matchingItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(matchingItem.getQuantity()));
                BigDecimal discount = itemTotal.multiply(BigDecimal.valueOf(pv.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                
                matchingItem.setAppliedVoucherCode(pv.getVoucherCode());
                matchingItem.setAppliedVoucherDiscount(discount);
                invoiceItemRepository.save(matchingItem);
            }
            
            InvoiceVoucher invoiceVoucher = new InvoiceVoucher();
            invoiceVoucher.setInvoice(invoice);
            invoiceVoucher.setVoucherScope("PRODUCT");
            invoiceVoucher.setProductVoucher(pv);
            invoiceVoucherRepository.save(invoiceVoucher);
        }
        
        // Decrement remaining quantity and save Combo vouchers
        for (ProductComboVoucher pcv : appliedComboVouchers) {
            pcv.setRemainingQuantity(pcv.getRemainingQuantity() - 1);
            
            // FIX #11: Auto-update is_active when remaining_quantity = 0
            if (pcv.getRemainingQuantity() <= 0) {
                pcv.setIsActive(false);
            }
            
            productComboVoucherRepository.save(pcv);
            
            InvoiceVoucher invoiceVoucher = new InvoiceVoucher();
            invoiceVoucher.setInvoice(invoice);
            invoiceVoucher.setVoucherScope("COMBO");
            invoiceVoucher.setProductComboVoucher(pcv);
            invoiceVoucherRepository.save(invoiceVoucher);
        }

        PaymentCheckoutResponse response = new PaymentCheckoutResponse();
        response.setInvoiceId(invoice.getId());
        response.setInvoiceCode(invoice.getInvoiceCode());
        response.setInvoiceStatus(invoice.getInvoiceStatus());
        response.setSubtotal(subtotal);
        response.setTotalDiscount(totalDiscount);
        response.setTaxAmount(taxAmount);
        response.setServiceFeeAmount(serviceFeeAmount);
        response.setTotalPayable(totalPayable);
        response.setPaidAt(invoice.getPaidAt());

        response.setCashReceived(cashReceived);
        response.setChangeDue(cashReceived.subtract(totalPayable).max(BigDecimal.ZERO));
        return response;
    }

    @Transactional
    public void updateItemQuantity(Integer itemId, Integer quantity) {
        InvoiceItem item = invoiceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        Invoice invoice = item.getInvoice();
        if (invoice == null || !STATUS_IN_PROGRESS.equals(invoice.getInvoiceStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is not editable");
        }

        if (quantity == null || quantity <= 0) {
            invoiceItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            invoiceItemRepository.save(item);
        }

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithItem(invoice.getId()).stream()
                .filter(i -> i.getStatus() != InvoiceItemStatus.CANCELLED)
                .toList();
        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        invoice.setSubtotalAmount(subtotal);
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void cancelByTable(Integer tableId) {
        Invoice invoice = getSingleInProgressInvoice(tableId);

        if (STATUS_PAID.equals(invoice.getInvoiceStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice already paid");
        }
        invoice.setInvoiceStatus(STATUS_CANCELLED);
        invoiceRepository.save(invoice);
        
        // Clear table status to AVAILABLE after cancellation (Requirements 4.2, 4.3, 4.4)
        List<InvoiceDiningTable> tableLinks = invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());
        List<Integer> tableIds = tableLinks.stream()
                .map(link -> link.getDiningTable().getId())
                .toList();
        
        if (!tableIds.isEmpty()) {
            diningTableRepository.updateTableStatusByIdIn(tableIds, "AVAILABLE");
            
            // Broadcast table status change via WebSocket (Requirement 4.2)
            tableStatusBroadcastService.broadcastTableStatusChange(tableIds, "AVAILABLE");
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
            String invoiceCodes = invoices.stream()
                    .map(Invoice::getInvoiceCode)
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                    "Table #" + tableId + " is attached to multiple active invoices: " + invoiceCodes);
        }
        
        return invoices.get(0);
    }

    /**
     * Auto-apply product vouchers to invoice items (PUBLIC PROMOTION)
     * ProductVoucher works as a public promotion - anyone (including walk-in customers) can use it
     * as long as the voucher is active and within the valid date range.
     * NO customer requirement - applies to ALL customers.
     * 
     * CRITICAL: This method now SAVES the discount to database to ensure consistency between
     * getPaymentByTable and checkout calculations.
     */
    private void autoApplyProductVouchers(List<InvoiceItem> items, List<PaymentItemResponse> itemResponses, Customer customer) {
        LocalDate today = LocalDate.now();
        
        // Load all active product vouchers (PUBLIC PROMOTION for everyone - no customer requirement)
        List<ProductVoucher> productVouchers = productVoucherRepository.findByIsActiveTrue();
        
        for (int i = 0; i < items.size(); i++) {
            InvoiceItem item = items.get(i);
            PaymentItemResponse itemResponse = itemResponses.get(i);
            
            // Skip if item already has voucher applied (from previous checkout attempt)
            if (item.getAppliedVoucherCode() != null) {
                itemResponse.setVoucherCode(item.getAppliedVoucherCode());
                itemResponse.setDiscount(item.getAppliedVoucherDiscount() != null ? item.getAppliedVoucherDiscount() : BigDecimal.ZERO);
                continue;
            }
            
            // Try to find matching product voucher (auto-apply if product has active promotion)
            if (item.getProduct() != null) {
                ProductVoucher matchingVoucher = productVouchers.stream()
                    .filter(pv -> pv.getProduct().getId().equals(item.getProduct().getId()))
                    .filter(pv -> pv.getRemainingQuantity() != null && pv.getRemainingQuantity() > 0)
                    .filter(pv -> pv.getValidFrom() == null || !today.isBefore(pv.getValidFrom()))
                    .filter(pv -> pv.getValidTo() == null || !today.isAfter(pv.getValidTo()))
                    .findFirst()
                    .orElse(null);
                
                if (matchingVoucher != null && matchingVoucher.getDiscountPercent() != null && matchingVoucher.getDiscountPercent() > 0) {
                    BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    BigDecimal discount = itemTotal.multiply(BigDecimal.valueOf(matchingVoucher.getDiscountPercent()))
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                    
                    // CRITICAL FIX: Save discount to database to ensure consistency
                    item.setAppliedVoucherCode(matchingVoucher.getVoucherCode());
                    item.setAppliedVoucherDiscount(discount);
                    invoiceItemRepository.save(item);
                    
                    // Also set in response for display
                    itemResponse.setVoucherCode(matchingVoucher.getVoucherCode());
                    itemResponse.setDiscount(discount);
                }
            }
        }
    }

    /**
     * Auto-apply invoice voucher (CustomerVoucher) when conditions are met
     * Finds the best available voucher that meets minimum order amount requirement
     * Priority: Highest ACTUAL discount amount (not just percentage)
     * Example: 30% off 500k (150k) is better than 15% off 1000k (150k) - same discount
     *          But 30% off 1000k (300k) is better than 15% off 1000k (150k)
     */
    private CustomerVoucher autoApplyInvoiceVoucher(Customer customer, BigDecimal subtotalAfterItemVoucher) {
        LocalDate today = LocalDate.now();
        List<CustomerVoucher> availableVouchers = new ArrayList<>();
        
        // 1. Load customer-specific vouchers if customer exists
        if (customer != null) {
            List<CustomerVoucher> customerVouchers = customerVoucherRepository.findByCustomerId(customer.getId());
            availableVouchers.addAll(customerVouchers);
        }
        
        // 2. Load public vouchers (customer_id = NULL) - available to everyone including walk-ins
        List<CustomerVoucher> publicVouchers = customerVoucherRepository.findPublicVouchers();
        availableVouchers.addAll(publicVouchers);
        
        // 3. Filter and find best voucher by ACTUAL discount amount
        CustomerVoucher bestVoucher = availableVouchers.stream()
            // Must be active
            .filter(cv -> VoucherStatus.HOAT_DONG.equals(cv.getVoucherStatus()))
            // Must have remaining uses
            .filter(cv -> cv.getRemainingQuantity() != null && cv.getRemainingQuantity() > 0)
            // Must not be expired
            .filter(cv -> cv.getExpiresAt() == null || !cv.getExpiresAt().isBefore(today))
            // Must meet minimum order amount requirement
            .filter(cv -> {
                BigDecimal minOrderAmount = cv.getPersonalVoucher().getMinOrderAmount();
                return minOrderAmount == null || subtotalAfterItemVoucher.compareTo(minOrderAmount) >= 0;
            })
            // Sort by ACTUAL discount amount (highest first)
            .max((cv1, cv2) -> {
                // Calculate actual discount for voucher 1
                Integer percent1 = cv1.getPersonalVoucher().getDiscountPercent();
                if (percent1 == null) percent1 = 0;
                BigDecimal discount1 = subtotalAfterItemVoucher
                    .multiply(BigDecimal.valueOf(percent1))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                
                // Calculate actual discount for voucher 2
                Integer percent2 = cv2.getPersonalVoucher().getDiscountPercent();
                if (percent2 == null) percent2 = 0;
                BigDecimal discount2 = subtotalAfterItemVoucher
                    .multiply(BigDecimal.valueOf(percent2))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
                
                // Compare actual discount amounts
                return discount1.compareTo(discount2);
            })
            .orElse(null);
        
        return bestVoucher;
    }

    private List<PaymentVoucherResponse> loadAvailableVouchers(Customer customer) {
        List<PaymentVoucherResponse> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // 1. Load Customer Vouchers (Personal vouchers for specific customer)
        if (customer != null) {
            List<CustomerVoucher> customerVouchers = customerVoucherRepository
                    .findByCustomerId(customer.getId());
            
            for (CustomerVoucher cv : customerVouchers) {
                boolean isExpired = cv.getExpiresAt() != null && cv.getExpiresAt().isBefore(today);
                String status;
                boolean isActive;
                
                if (cv.getRemainingQuantity() == null || cv.getRemainingQuantity() <= 0) {
                    status = "USED";
                    isActive = false;
                } else if (VoucherStatus.DA_DUNG.equals(cv.getVoucherStatus())) {
                    status = "USED";
                    isActive = false;
                } else if (isExpired) {
                    status = "EXPIRED";
                    isActive = false;
                } else if (VoucherStatus.HOAT_DONG.equals(cv.getVoucherStatus())) {
                    status = "ACTIVE";
                    isActive = true;
                } else {
                    status = "INACTIVE";
                    isActive = false;
                }
                
                PaymentVoucherResponse dto = new PaymentVoucherResponse();
                dto.setId(cv.getId());
                dto.setCode(cv.getPersonalVoucher().getVoucherCode());
                dto.setName(cv.getPersonalVoucher().getVoucherName());
                dto.setPercent(cv.getPersonalVoucher().getDiscountPercent());
                dto.setExpiresAt(cv.getExpiresAt());
                dto.setRemainingQuantity(cv.getRemainingQuantity());
                dto.setVoucherStatus(status);
                dto.setVoucherType("CUSTOMER");
                dto.setApplicableItemId(null);
                dto.setApplicableItemName(null);
                
                if (isActive) {
                    result.add(0, dto); // Add active vouchers at the beginning
                } else {
                    result.add(dto); // Add used/expired/inactive vouchers at the end
                }
            }
        }
        
        // 1.5. Load Public Customer Vouchers (customer_id = NULL - applies to all customers including walk-ins)
        List<CustomerVoucher> publicVouchers = customerVoucherRepository.findPublicVouchers();
        for (CustomerVoucher cv : publicVouchers) {
            boolean isExpired = cv.getExpiresAt() != null && cv.getExpiresAt().isBefore(today);
            String status;
            boolean isActive;
            
            if (cv.getRemainingQuantity() == null || cv.getRemainingQuantity() <= 0) {
                status = "USED";
                isActive = false;
            } else if (VoucherStatus.DA_DUNG.equals(cv.getVoucherStatus())) {
                status = "USED";
                isActive = false;
            } else if (isExpired) {
                status = "EXPIRED";
                isActive = false;
            } else if (VoucherStatus.HOAT_DONG.equals(cv.getVoucherStatus())) {
                status = "ACTIVE";
                isActive = true;
            } else {
                status = "INACTIVE";
                isActive = false;
            }
            
            PaymentVoucherResponse dto = new PaymentVoucherResponse();
            dto.setId(cv.getId());
            dto.setCode(cv.getPersonalVoucher().getVoucherCode());
            dto.setName(cv.getPersonalVoucher().getVoucherName());
            dto.setPercent(cv.getPersonalVoucher().getDiscountPercent());
            dto.setExpiresAt(cv.getExpiresAt());
            dto.setRemainingQuantity(cv.getRemainingQuantity());
            dto.setVoucherStatus(status);
            dto.setVoucherType("CUSTOMER");
            dto.setApplicableItemId(null);
            dto.setApplicableItemName(null);
            
            if (isActive) {
                result.add(0, dto); // Add active vouchers at the beginning
            } else {
                result.add(dto); // Add used/expired/inactive vouchers at the end
            }
        }
        
        // 2. Load Product Vouchers (Vouchers for specific products)
        List<ProductVoucher> productVouchers = productVoucherRepository.findByIsActiveTrue();
        for (ProductVoucher pv : productVouchers) {
            boolean isExpired = (pv.getValidTo() != null && pv.getValidTo().isBefore(today)) ||
                               (pv.getValidFrom() != null && pv.getValidFrom().isAfter(today));
            boolean hasRemainingUses = pv.getRemainingQuantity() != null && pv.getRemainingQuantity() > 0;
            String status;
            boolean isActive;
            
            if (isExpired) {
                status = "EXPIRED";
                isActive = false;
            } else if (!hasRemainingUses) {
                status = "USED";
                isActive = false;
            } else if (pv.getIsActive() != null && pv.getIsActive()) {
                status = "ACTIVE";
                isActive = true;
            } else {
                status = "INACTIVE";
                isActive = false;
            }
            
            PaymentVoucherResponse dto = new PaymentVoucherResponse();
            dto.setId(pv.getId());
            dto.setCode(pv.getVoucherCode());
            dto.setName(pv.getVoucherName());
            dto.setPercent(pv.getDiscountPercent());
            dto.setExpiresAt(pv.getValidTo());
            dto.setRemainingQuantity(pv.getRemainingQuantity());
            dto.setVoucherStatus(status);
            dto.setVoucherType("PRODUCT");
            dto.setApplicableItemId(pv.getProduct().getId());
            dto.setApplicableItemName(pv.getProduct().getProductName());
            
            if (isActive) {
                result.add(0, dto); // Add active vouchers at the beginning
            } else {
                result.add(dto); // Add expired/inactive vouchers at the end
            }
        }
        
        // NOTE: ProductComboVoucher is NOT included in the voucher selection list
        // Combo vouchers auto-apply when a combo is in the order (not manually selectable)
        
        return result;
    }
}
