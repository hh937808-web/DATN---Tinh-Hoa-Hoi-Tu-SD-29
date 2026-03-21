package com.example.datn_sd_29.audit.repository;

import com.example.datn_sd_29.audit.document.AuditLogDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogMongoRepository extends MongoRepository<AuditLogDocument, String> {

    // Find by user email
    Page<AuditLogDocument> findByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);

    // Find by user role
    Page<AuditLogDocument> findByUserRoleOrderByCreatedAtDesc(String userRole, Pageable pageable);

    // Find by action type
    Page<AuditLogDocument> findByActionTypeOrderByCreatedAtDesc(String actionType, Pageable pageable);

    // Find by entity type
    Page<AuditLogDocument> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);

    // Find by entity type and entity id
    List<AuditLogDocument> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, String entityId);

    // Find by severity
    Page<AuditLogDocument> findBySeverityOrderByCreatedAtDesc(String severity, Pageable pageable);

    // Find by date range
    Page<AuditLogDocument> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant startDate, Instant endDate, Pageable pageable);

    // Find by user email and date range
    Page<AuditLogDocument> findByUserEmailAndCreatedAtBetweenOrderByCreatedAtDesc(
            String userEmail, Instant startDate, Instant endDate, Pageable pageable);

    // Find by action type and date range
    Page<AuditLogDocument> findByActionTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            String actionType, Instant startDate, Instant endDate, Pageable pageable);

    // Find failed login attempts
    @Query("{ 'actionType': 'LOGIN_FAILED', 'createdAt': { $gte: ?0 } }")
    List<AuditLogDocument> findFailedLoginsSince(Instant since);

    // Find critical events
    @Query("{ 'severity': 'CRITICAL' }")
    Page<AuditLogDocument> findCriticalEvents(Pageable pageable);

    // Count actions by user in time range
    @Query(value = "{ 'userEmail': ?0, 'createdAt': { $gte: ?1, $lte: ?2 } }", count = true)
    Long countUserActionsInRange(String userEmail, Instant startDate, Instant endDate);
}
