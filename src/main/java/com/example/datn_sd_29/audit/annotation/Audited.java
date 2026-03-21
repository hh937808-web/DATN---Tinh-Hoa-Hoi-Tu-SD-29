package com.example.datn_sd_29.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited.
 * When applied to a controller method, the AuditAspect will automatically
 * create an audit log entry with request/response details.
 * 
 * Usage:
 * @Audited(actionType = "CREATE", entityType = "Invoice", description = "Tạo hóa đơn mới")
 * public ResponseEntity<?> createInvoice(@RequestBody InvoiceRequest request) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    
    /**
     * Type of action being performed (e.g., CREATE, UPDATE, DELETE, LOGIN)
     */
    String actionType();
    
    /**
     * Type of entity being affected (e.g., Invoice, Customer, Employee)
     * Optional - can be left empty for non-entity actions
     */
    String entityType() default "";
    
    /**
     * Description of the action in Vietnamese
     * Optional - will be auto-generated if not provided
     */
    String description() default "";
    
    /**
     * Severity level of the action
     * Default is INFO
     */
    String severity() default "INFO";
}
