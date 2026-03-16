package com.example.datn_sd_29.product_combo.repository;

import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductComboRepository extends JpaRepository<ProductCombo, Integer> {

    List<ProductCombo> findByComboNameContainingIgnoreCase(String name);


    List<ProductCombo> findByComboPrice(BigDecimal price);


    List<ProductCombo> findByIsActive(Boolean status);


    @Query("""
        SELECT DISTINCT pci.productCombo
        FROM ProductComboItem pci
        JOIN pci.product p
        WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :productName, '%'))
    """)
    List<ProductCombo> findByProductName(@Param("productName") String productName);



    List<ProductCombo> findAllByOrderByComboPriceAsc();
    List<ProductCombo> findAllByOrderByComboPriceDesc();


    List<ProductCombo> findAllByOrderByCreatedAtAsc();
    List<ProductCombo> findAllByOrderByCreatedAtDesc();
}