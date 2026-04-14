package com.example.datn_sd_29.product_combo.repository;

import com.example.datn_sd_29.product_combo.entity.ProductComboItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductComboItemRepository extends JpaRepository<ProductComboItem, Integer> {
    
    @Query("SELECT pci FROM ProductComboItem pci " +
           "JOIN FETCH pci.product " +
           "WHERE pci.productCombo.id = :comboId")
    List<ProductComboItem> findByProductComboId(@Param("comboId") Integer comboId);
    
    void deleteByProductComboId(Integer comboId);
}

