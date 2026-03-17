package com.example.datn_sd_29.invoice.service;

import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.invoice.dto.PaymentCheckoutRequest;
import com.example.datn_sd_29.invoice.dto.PaymentCheckoutResponse;
import com.example.datn_sd_29.invoice.dto.PaymentDetailResponse;
import com.example.datn_sd_29.invoice.dto.PaymentItemResponse;
import com.example.datn_sd_29.invoice.dto.PaymentVoucherResponse;
import com.example.datn_sd_29.invoice.entity.Invoice;
import com.example.datn_sd_29.invoice.entity.InvoiceDiningTable;
import com.example.datn_sd_29.invoice.entity.InvoiceItem;
import com.example.datn_sd_29.invoice.entity.InvoicePayment;
import com.example.datn_sd_29.invoice.entity.InvoiceVoucher;
import com.example.datn_sd_29.invoice.repository.InvoiceDiningTableRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceItemRepository;
import com.example.datn_sd_29.invoice.repository.InvoicePaymentRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceRepository;
import com.example.datn_sd_29.invoice.repository.InvoiceVoucherRepository;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import com.example.datn_sd_29.voucher.entity.CustomerVoucher;
import com.example.datn_sd_29.voucher.repository.CustomerVoucherRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final BigDecimal POINT_VALUE = BigDecimal.valueOf(1000);
    private static final BigDecimal EARN_POINT_DIVISOR = BigDecimal.valueOf(10000);

    private final InvoiceDiningTableRepository invoiceDiningTableRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final InvoiceVoucherRepository invoiceVoucherRepository;
    private final CustomerRepository customerRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;

    @Transactional(readOnly = true)
    public PaymentDetailResponse getPaymentByTable(Integer tableId) {
        Invoice invoice = invoiceDiningTableRepository
                .findInvoiceByTableAndStatus(tableId, STATUS_IN_PROGRESS)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No active invoice for this table"));

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithItem(invoice.getId());
        List<InvoiceDiningTable> tableLinks =
                invoiceDiningTableRepository.findByInvoiceIdWithTable(invoice.getId());

        PaymentDetailResponse response = new PaymentDetailResponse();
        response.setInvoiceId(invoice.getId());
        response.setInvoiceCode(invoice.getInvoiceCode());
        response.setInvoiceStatus(invoice.getInvoiceStatus());
        response.setReservedAt(invoice.getReservedAt());
        response.setCheckedInAt(invoice.getCheckedInAt());
        response.setGuestCount(invoice.getGuestCount());

        Customer customer = invoice.getCustomer();
        if (customer == null) {
            response.setCustomerType("GUEST");
            response.setCustomerName("Khách lẻ");
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
            if (item.getProduct() != null) {
                i.setName(item.getProduct().getProductName());
                i.setType("PRODUCT");
            } else if (item.getProductCombo() != null) {
                i.setName(item.getProductCombo().getComboName());
                i.setType("COMBO");
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

        response.setItems(itemResponses);
        response.setSubtotal(subtotal);
        response.setItemVoucherDiscount(BigDecimal.ZERO);
        response.setManualDiscountPercent(invoice.getManualDiscountPercent());
        response.setManualDiscountAmount(invoice.getManualDiscountAmount());
        response.setTaxPercent(invoice.getTaxPercent());
        response.setServiceFeePercent(invoice.getServiceFeePercent());
        response.setPointValue(POINT_VALUE.intValue());

        response.setVouchers(loadAvailableVouchers(customer));
        return response;
    }

    @Transactional
    public PaymentCheckoutResponse checkout(PaymentCheckoutRequest request) {
        Invoice invoice = invoiceDiningTableRepository
                .findInvoiceByTableAndStatus(request.getTableId(), STATUS_IN_PROGRESS)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No active invoice for this table"));

        if (STATUS_PAID.equals(invoice.getInvoiceStatus()) || STATUS_CANCELLED.equals(invoice.getInvoiceStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is closed");
        }

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithItem(invoice.getId());
        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal itemVoucherDiscount = BigDecimal.ZERO;
        Customer customer = invoice.getCustomer();

        int usePoints = request.getUsePoints() == null ? 0 : request.getUsePoints();
        int customerPoints = customer == null || customer.getLoyaltyPoints() == null
                ? 0
                : customer.getLoyaltyPoints();
        if (usePoints > customerPoints) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough points");
        }

        BigDecimal invoiceVoucherDiscount = BigDecimal.ZERO;
        CustomerVoucher customerVoucher = null;
        if (request.getCustomerVoucherId() != null && customer != null) {
            customerVoucher = customerVoucherRepository
                    .findByIdAndCustomerId(request.getCustomerVoucherId(), customer.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found"));

            if (!"ACTIVE".equalsIgnoreCase(customerVoucher.getVoucherStatus())
                    || customerVoucher.getRemainingQuantity() == null
                    || customerVoucher.getRemainingQuantity() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher is not available");
            }

            if (customerVoucher.getExpiresAt() != null
                    && customerVoucher.getExpiresAt().isBefore(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher expired");
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

        BigDecimal manualDiscountPercent = request.getManualDiscountPercent() == null
                ? BigDecimal.ZERO
                : request.getManualDiscountPercent();
        BigDecimal manualDiscountAmount = request.getManualDiscountAmount() == null
                ? BigDecimal.ZERO
                : request.getManualDiscountAmount();

        BigDecimal manualDiscount = BigDecimal.ZERO;
        if (manualDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            manualDiscount = manualDiscountAmount;
        } else if (manualDiscountPercent.compareTo(BigDecimal.ZERO) > 0) {
            manualDiscount = baseAfterVoucher.multiply(manualDiscountPercent)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        }
        if (manualDiscount.compareTo(baseAfterVoucher) > 0) {
            manualDiscount = baseAfterVoucher;
        }

        BigDecimal baseAfterManual = baseAfterVoucher.subtract(manualDiscount);

        BigDecimal pointsDiscount = POINT_VALUE.multiply(BigDecimal.valueOf(usePoints));
        if (pointsDiscount.compareTo(baseAfterManual) > 0) {
            pointsDiscount = baseAfterManual.max(BigDecimal.ZERO);
        }

        BigDecimal taxableBase = baseAfterManual.subtract(pointsDiscount);
        if (taxableBase.compareTo(BigDecimal.ZERO) < 0) {
            taxableBase = BigDecimal.ZERO;
        }

        BigDecimal taxPercent = request.getTaxPercent() == null ? BigDecimal.ZERO : request.getTaxPercent();
        BigDecimal serviceFeePercent = request.getServiceFeePercent() == null
                ? BigDecimal.ZERO
                : request.getServiceFeePercent();

        BigDecimal taxAmount = BigDecimal.ZERO;
        if (taxPercent.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = taxableBase.multiply(taxPercent)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        }

        BigDecimal serviceFeeAmount = BigDecimal.ZERO;
        if (serviceFeePercent.compareTo(BigDecimal.ZERO) > 0) {
            serviceFeeAmount = taxableBase.multiply(serviceFeePercent)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
        }

        BigDecimal totalDiscount = itemVoucherDiscount
                .add(invoiceVoucherDiscount)
                .add(manualDiscount)
                .add(pointsDiscount);
        BigDecimal totalPayable = taxableBase.add(taxAmount).add(serviceFeeAmount);

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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total paid is not enough");
            }
            if (totalPaid.compareTo(totalPayable) > 0 && cashReceived.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Overpay requires cash method");
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
        invoice.setManualDiscountPercent(manualDiscountPercent);
        invoice.setManualDiscountAmount(manualDiscount);
        invoice.setTaxPercent(taxPercent);
        invoice.setTaxAmount(taxAmount);
        invoice.setServiceFeePercent(serviceFeePercent);
        invoice.setServiceFeeAmount(serviceFeeAmount);
        invoice.setPaymentMethod(finalMethod);
        invoice.setPaidAt(Instant.now());
        invoice.setInvoiceStatus(STATUS_PAID);
        invoice.setUsedPoints(usePoints);

        int earnedPoints = totalPayable.divide(EARN_POINT_DIVISOR, 0, RoundingMode.FLOOR).intValue();
        invoice.setEarnedPoints(earnedPoints);

        invoiceRepository.save(invoice);

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
            int remain = customerVoucher.getRemainingQuantity() == null ? 0 : customerVoucher.getRemainingQuantity();
            customerVoucher.setRemainingQuantity(Math.max(0, remain - 1));
            if (customerVoucher.getRemainingQuantity() != null && customerVoucher.getRemainingQuantity() <= 0) {
                customerVoucher.setVoucherStatus("INACTIVE");
            }
            customerVoucherRepository.save(customerVoucher);

            InvoiceVoucher invoiceVoucher = new InvoiceVoucher();
            invoiceVoucher.setInvoice(invoice);
            invoiceVoucher.setVoucherScope("CUSTOMER");
            invoiceVoucher.setCustomerVoucher(customerVoucher);
            invoiceVoucherRepository.save(invoiceVoucher);
        }

        PaymentCheckoutResponse response = new PaymentCheckoutResponse();
        response.setInvoiceId(invoice.getId());
        response.setInvoiceCode(invoice.getInvoiceCode());
        response.setInvoiceStatus(invoice.getInvoiceStatus());
        response.setSubtotal(subtotal);
        response.setTotalDiscount(totalDiscount);
        response.setManualDiscount(manualDiscount);
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

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdWithItem(invoice.getId());
        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        invoice.setSubtotalAmount(subtotal);
        invoiceRepository.save(invoice);
    }

    @Transactional
    public void cancelByTable(Integer tableId) {
        Invoice invoice = invoiceDiningTableRepository
                .findInvoiceByTableAndStatus(tableId, STATUS_IN_PROGRESS)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No active invoice for this table"));

        if (STATUS_PAID.equals(invoice.getInvoiceStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice already paid");
        }
        invoice.setInvoiceStatus(STATUS_CANCELLED);
        invoiceRepository.save(invoice);
    }

    private List<PaymentVoucherResponse> loadAvailableVouchers(Customer customer) {
        if (customer == null) return List.of();
        List<CustomerVoucher> vouchers = customerVoucherRepository
                .findByCustomerIdAndVoucherStatus(customer.getId(), "ACTIVE");
        List<PaymentVoucherResponse> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (CustomerVoucher v : vouchers) {
            if (v.getRemainingQuantity() == null || v.getRemainingQuantity() < 1) continue;
            if (v.getExpiresAt() != null && v.getExpiresAt().isBefore(today)) continue;
            PaymentVoucherResponse dto = new PaymentVoucherResponse();
            dto.setId(v.getId());
            dto.setCode(v.getPersonalVoucher().getVoucherCode());
            dto.setName(v.getPersonalVoucher().getVoucherName());
            dto.setPercent(v.getPersonalVoucher().getDiscountPercent());
            dto.setExpiresAt(v.getExpiresAt());
            dto.setRemainingQuantity(v.getRemainingQuantity());
            result.add(dto);
        }
        return result;
    }
}
