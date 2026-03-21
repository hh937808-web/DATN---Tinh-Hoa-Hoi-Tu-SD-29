-- Migration: Add performance indexes for overtime monitoring
-- Feature: Table Overtime Smart Alerts
-- Requirements: 7.2

-- Index for overtime detection query
-- Optimizes queries that filter by IN_PROGRESS status and checked_in_at timestamp
CREATE NONCLUSTERED INDEX idx_invoice_status_checkedin 
ON invoice(invoice_status, checked_in_at)
WHERE invoice_status = 'IN_PROGRESS';

-- Index for next reservation lookup
-- Optimizes queries that find RESERVED reservations within time windows
CREATE NONCLUSTERED INDEX idx_invoice_reservation_time 
ON invoice(reserved_at, invoice_status)
WHERE invoice_status = 'RESERVED';

-- Index for table-invoice relationship lookup
-- Optimizes joins between invoice and dining_table via invoice_dining_table
CREATE NONCLUSTERED INDEX idx_invoice_dining_table_lookup 
ON invoice_dining_table(invoice_id, dining_table_id);
