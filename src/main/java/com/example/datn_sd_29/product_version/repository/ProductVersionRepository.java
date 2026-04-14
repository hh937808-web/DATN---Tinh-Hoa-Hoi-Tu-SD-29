package com.example.datn_sd_29.product_version.repository;

import com.example.datn_sd_29.product_version.document.ProductVersionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVersionRepository extends MongoRepository<ProductVersionDocument, String> {
}
