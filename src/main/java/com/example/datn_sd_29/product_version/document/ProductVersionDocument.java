package com.example.datn_sd_29.product_version.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_versions")
public class ProductVersionDocument {
    
    @Id
    private String id;
    
    private Integer productId;
    private Integer productComboId;
    private String itemType;
    private String itemName;
    private BigDecimal unitPrice;
    private String description;
    private String category;
    
    private List<ComboItemSnapshot> comboItems;
    
    private Instant createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboItemSnapshot {
        private Integer productId;
        private String productName;
        private Integer quantity;
    }
}
