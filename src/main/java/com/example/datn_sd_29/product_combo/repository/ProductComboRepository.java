package com.example.datn_sd_29.product_combo.repository;

import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductComboRepository extends JpaRepository<ProductCombo, Integer> {
    List<ProductCombo> findByIsActiveTrue();

    boolean existsByComboCode(String comboCode);

    boolean existsByComboCodeAndIdNot(String comboCode, Integer id);
}
