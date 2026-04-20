package com.example.datn_sd_29.querybuilder.repository;

import com.example.datn_sd_29.querybuilder.entity.SavedQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedQueryRepository extends JpaRepository<SavedQuery, Long> {
    List<SavedQuery> findByCreatedByOrderByUpdatedAtDesc(Integer createdBy);
    List<SavedQuery> findAllByOrderByUpdatedAtDesc();
}
