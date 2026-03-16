package com.example.datn_sd_29.voucher.repository;

import com.example.datn_sd_29.voucher.entity.ProductVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVoucherRepository extends JpaRepository<ProductVoucher, Integer> {

        @Query("""
        SELECT pv
        FROM ProductVoucher pv
        LEFT JOIN pv.product p
        WHERE (:code IS NULL OR :code = '' OR LOWER(pv.voucherCode) LIKE LOWER(CONCAT('%', :code, '%')))
        AND (:name IS NULL OR :name = '' OR LOWER(pv.voucherName) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:productName IS NULL OR :productName = '' OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :productName, '%')))
        AND (:percent IS NULL OR pv.discountPercent = :percent)
        AND (:status IS NULL OR pv.isActive = :status)
    """)
        List<ProductVoucher> searchVoucher(
                @Param("code") String code,
                @Param("name") String name,
                @Param("productName") String productName,
                @Param("percent") Integer percent,
                @Param("status") Boolean status
        );


        List<ProductVoucher> findAllByOrderByIdAsc();
        List<ProductVoucher> findAllByOrderByIdDesc();

        List<ProductVoucher> findAllByOrderByDiscountPercentAsc();
        List<ProductVoucher> findAllByOrderByDiscountPercentDesc();

        List<ProductVoucher> findAllByOrderByRemainingQuantityAsc();
        List<ProductVoucher> findAllByOrderByRemainingQuantityDesc();

        List<ProductVoucher> findAllByOrderByCreatedAtAsc();
        List<ProductVoucher> findAllByOrderByCreatedAtDesc();


    @Query("""
SELECT pv
FROM ProductVoucher pv
ORDER BY (pv.validTo - pv.validFrom) ASC
""")
    List<ProductVoucher> sortDurationAsc();

    @Query("""
SELECT pv
FROM ProductVoucher pv
ORDER BY (pv.validTo - pv.validFrom) DESC
""")
    List<ProductVoucher> sortDurationDesc();
}