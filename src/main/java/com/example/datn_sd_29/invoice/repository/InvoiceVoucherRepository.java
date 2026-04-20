package com.example.datn_sd_29.invoice.repository;

import com.example.datn_sd_29.invoice.entity.InvoiceVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceVoucherRepository extends JpaRepository<InvoiceVoucher, Integer> {

    @Query("SELECT iv FROM InvoiceVoucher iv " +
           "LEFT JOIN FETCH iv.customerVoucher cv LEFT JOIN FETCH cv.personalVoucher " +
           "LEFT JOIN FETCH iv.productVoucher pv LEFT JOIN FETCH pv.product " +
           "LEFT JOIN FETCH iv.productComboVoucher pcv LEFT JOIN FETCH pcv.productCombo " +
           "WHERE iv.invoice.id = :invoiceId")
    List<InvoiceVoucher> findByInvoiceId(@Param("invoiceId") Integer invoiceId);
}
