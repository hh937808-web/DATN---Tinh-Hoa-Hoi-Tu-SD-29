-- Migration: Add performance indexes for overtime monitoring
-- Feature: Table Overtime Smart Alerts
-- Requirements: 7.2

-- Index for overtime detection query (only if invoice table exists)
-- Optimizes queries that filter by IN_PROGRESS status and checked_in_at timestamp
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[invoice]') AND type in (N'U'))
BEGIN
    IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_invoice_status_checkedin' AND object_id = OBJECT_ID('invoice'))
    BEGIN
        CREATE NONCLUSTERED INDEX idx_invoice_status_checkedin 
        ON invoice(invoice_status, checked_in_at)
        WHERE invoice_status = 'IN_PROGRESS';
    END
END

-- Index for next reservation lookup (only if invoice table exists)
-- Optimizes queries that find RESERVED reservations within time windows
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[invoice]') AND type in (N'U'))
BEGIN
    IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_invoice_reservation_time' AND object_id = OBJECT_ID('invoice'))
    BEGIN
        CREATE NONCLUSTERED INDEX idx_invoice_reservation_time 
        ON invoice(reserved_at, invoice_status)
        WHERE invoice_status = 'RESERVED';
    END
END

-- Index for table-invoice relationship lookup (only if invoice_dining_table exists)
-- Optimizes joins between invoice and dining_table via invoice_dining_table
IF EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[invoice_dining_table]') AND type in (N'U'))
BEGIN
    IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_invoice_dining_table_lookup' AND object_id = OBJECT_ID('invoice_dining_table'))
    BEGIN
        CREATE NONCLUSTERED INDEX idx_invoice_dining_table_lookup 
        ON invoice_dining_table(invoice_id, dining_table_id);
    END
END
