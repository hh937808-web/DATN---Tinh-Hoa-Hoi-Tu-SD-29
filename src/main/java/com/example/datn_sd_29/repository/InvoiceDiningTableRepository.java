package com.example.datn_sd_29.repository;

import com.example.datn_sd_29.entity.InvoiceDiningTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceDiningTableRepository extends JpaRepository<InvoiceDiningTable, Integer> {

}
