-- =====================================================
-- DELETE EXTRA TABLES (70 -> 50) AND UPDATE REMAINING
-- =====================================================
-- This script will:
-- 1. Delete tables 51-70 (20 tables)
-- 2. Keep tables 1-50
-- 3. Update table names to format: A-1, A-2, B-1, B-2, etc.
-- 4. Set area and floor for each table
--
-- Total: 50 tables = 280 seats
-- - 9 tables x 2 seats = 18 seats
-- - 14 tables x 4 seats = 56 seats
-- - 13 tables x 6 seats = 78 seats
-- - 6 tables x 8 seats = 48 seats
-- - 8 tables x 10 seats = 80 seats
--
-- IMPORTANT: This will delete InvoiceItem records linked to tables 51-70!
-- Run this script manually in SQL Server Management Studio
-- =====================================================

USE DATN_SD29_ByHat;
GO

PRINT '=== Starting Table Cleanup and Update ==='
PRINT 'Current database: ' + DB_NAME()
GO

-- Step 1: Check current table count
PRINT 'Step 1: Checking current table count...'
SELECT COUNT(*) as current_table_count FROM DiningTable;
GO

-- Step 2: Delete InvoiceItem records linked to tables 51-70
PRINT 'Step 2: Deleting InvoiceItem records for tables 51-70...'
DELETE FROM InvoiceItem 
WHERE dining_table_id IN (SELECT dining_table_id FROM DiningTable WHERE dining_table_id > 50);
PRINT '-> InvoiceItem records deleted'
GO

-- Step 3: Delete InvoiceDiningTable records linked to tables 51-70
PRINT 'Step 3: Deleting InvoiceDiningTable records for tables 51-70...'
DELETE FROM InvoiceDiningTable 
WHERE dining_table_id IN (SELECT dining_table_id FROM DiningTable WHERE dining_table_id > 50);
PRINT '-> InvoiceDiningTable records deleted'
GO

-- Step 4: Delete tables 51-70
PRINT 'Step 4: Deleting tables 51-70...'
DELETE FROM DiningTable WHERE dining_table_id > 50;
PRINT '-> Extra tables deleted'
GO

-- Step 5: Verify table count
PRINT 'Step 5: Verifying table count...'
SELECT COUNT(*) as remaining_table_count FROM DiningTable;
GO

-- Step 6: Update table names and assign area/floor
-- Distribution: 49 tables total
-- Tầng 1: Khu A (12 bàn), Khu B (15 bàn), Khu C (12 bàn), Khu D (5 bàn) = 44 bàn
-- Tầng 2: Khu E (5 bàn) = 5 bàn
PRINT 'Step 6: Updating table names, area, and floor...'

-- Khu A (Tầng 1): Bàn 1-12 (12 bàn)
UPDATE DiningTable SET table_name = 'A-1', area = 'A', floor = 1 WHERE dining_table_id = 1;
UPDATE DiningTable SET table_name = 'A-2', area = 'A', floor = 1 WHERE dining_table_id = 2;
UPDATE DiningTable SET table_name = 'A-3', area = 'A', floor = 1 WHERE dining_table_id = 3;
UPDATE DiningTable SET table_name = 'A-4', area = 'A', floor = 1 WHERE dining_table_id = 4;
UPDATE DiningTable SET table_name = 'A-5', area = 'A', floor = 1 WHERE dining_table_id = 5;
UPDATE DiningTable SET table_name = 'A-6', area = 'A', floor = 1 WHERE dining_table_id = 6;
UPDATE DiningTable SET table_name = 'A-7', area = 'A', floor = 1 WHERE dining_table_id = 7;
UPDATE DiningTable SET table_name = 'A-8', area = 'A', floor = 1 WHERE dining_table_id = 8;
UPDATE DiningTable SET table_name = 'A-9', area = 'A', floor = 1 WHERE dining_table_id = 9;
UPDATE DiningTable SET table_name = 'A-10', area = 'A', floor = 1 WHERE dining_table_id = 10;
UPDATE DiningTable SET table_name = 'A-11', area = 'A', floor = 1 WHERE dining_table_id = 11;
UPDATE DiningTable SET table_name = 'A-12', area = 'A', floor = 1 WHERE dining_table_id = 12;

-- Khu B (Tầng 1): Bàn 13-27 (15 bàn)
UPDATE DiningTable SET table_name = 'B-1', area = 'B', floor = 1 WHERE dining_table_id = 13;
UPDATE DiningTable SET table_name = 'B-2', area = 'B', floor = 1 WHERE dining_table_id = 14;
UPDATE DiningTable SET table_name = 'B-3', area = 'B', floor = 1 WHERE dining_table_id = 15;
UPDATE DiningTable SET table_name = 'B-4', area = 'B', floor = 1 WHERE dining_table_id = 16;
UPDATE DiningTable SET table_name = 'B-5', area = 'B', floor = 1 WHERE dining_table_id = 17;
UPDATE DiningTable SET table_name = 'B-6', area = 'B', floor = 1 WHERE dining_table_id = 18;
UPDATE DiningTable SET table_name = 'B-7', area = 'B', floor = 1 WHERE dining_table_id = 19;
UPDATE DiningTable SET table_name = 'B-8', area = 'B', floor = 1 WHERE dining_table_id = 20;
UPDATE DiningTable SET table_name = 'B-9', area = 'B', floor = 1 WHERE dining_table_id = 21;
UPDATE DiningTable SET table_name = 'B-10', area = 'B', floor = 1 WHERE dining_table_id = 22;
UPDATE DiningTable SET table_name = 'B-11', area = 'B', floor = 1 WHERE dining_table_id = 23;
UPDATE DiningTable SET table_name = 'B-12', area = 'B', floor = 1 WHERE dining_table_id = 24;
UPDATE DiningTable SET table_name = 'B-13', area = 'B', floor = 1 WHERE dining_table_id = 25;
UPDATE DiningTable SET table_name = 'B-14', area = 'B', floor = 1 WHERE dining_table_id = 26;
UPDATE DiningTable SET table_name = 'B-15', area = 'B', floor = 1 WHERE dining_table_id = 27;

-- Khu C (Tầng 1): Bàn 28-39 (12 bàn)
UPDATE DiningTable SET table_name = 'C-1', area = 'C', floor = 1 WHERE dining_table_id = 28;
UPDATE DiningTable SET table_name = 'C-2', area = 'C', floor = 1 WHERE dining_table_id = 29;
UPDATE DiningTable SET table_name = 'C-3', area = 'C', floor = 1 WHERE dining_table_id = 30;
UPDATE DiningTable SET table_name = 'C-4', area = 'C', floor = 1 WHERE dining_table_id = 31;
UPDATE DiningTable SET table_name = 'C-5', area = 'C', floor = 1 WHERE dining_table_id = 32;
UPDATE DiningTable SET table_name = 'C-6', area = 'C', floor = 1 WHERE dining_table_id = 33;
UPDATE DiningTable SET table_name = 'C-7', area = 'C', floor = 1 WHERE dining_table_id = 34;
UPDATE DiningTable SET table_name = 'C-8', area = 'C', floor = 1 WHERE dining_table_id = 35;
UPDATE DiningTable SET table_name = 'C-9', area = 'C', floor = 1 WHERE dining_table_id = 36;
UPDATE DiningTable SET table_name = 'C-10', area = 'C', floor = 1 WHERE dining_table_id = 37;
UPDATE DiningTable SET table_name = 'C-11', area = 'C', floor = 1 WHERE dining_table_id = 38;
UPDATE DiningTable SET table_name = 'C-12', area = 'C', floor = 1 WHERE dining_table_id = 39;

-- Khu D (Tầng 1): Bàn 40-44 (5 bàn)
UPDATE DiningTable SET table_name = 'D-1', area = 'D', floor = 1 WHERE dining_table_id = 40;
UPDATE DiningTable SET table_name = 'D-2', area = 'D', floor = 1 WHERE dining_table_id = 41;
UPDATE DiningTable SET table_name = 'D-3', area = 'D', floor = 1 WHERE dining_table_id = 42;
UPDATE DiningTable SET table_name = 'D-4', area = 'D', floor = 1 WHERE dining_table_id = 43;
UPDATE DiningTable SET table_name = 'D-5', area = 'D', floor = 1 WHERE dining_table_id = 44;

-- Khu E (Tầng 2): Bàn 45-49 (5 bàn)
UPDATE DiningTable SET table_name = 'E-1', area = 'E', floor = 2 WHERE dining_table_id = 45;
UPDATE DiningTable SET table_name = 'E-2', area = 'E', floor = 2 WHERE dining_table_id = 46;
UPDATE DiningTable SET table_name = 'E-3', area = 'E', floor = 2 WHERE dining_table_id = 47;
UPDATE DiningTable SET table_name = 'E-4', area = 'E', floor = 2 WHERE dining_table_id = 48;
UPDATE DiningTable SET table_name = 'E-5', area = 'E', floor = 2 WHERE dining_table_id = 49;

PRINT '-> Table names, area, and floor updated'
GO

-- Step 7: Verify final result
PRINT 'Step 7: Verifying final result...'
SELECT 
    dining_table_id,
    table_name,
    seating_capacity,
    area,
    floor,
    table_status
FROM DiningTable
ORDER BY dining_table_id;
GO

PRINT '=== DONE! ==='
PRINT 'Tables 50-70 have been deleted'
PRINT '49 tables remain with proper naming (A-1 to E-5)'
PRINT 'Area and floor information has been set'
GO
