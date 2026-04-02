-- =====================================================
-- DELETE 20 TABLES (70 -> 50)
-- =====================================================
USE DATN_SD29_ByHat;
GO

PRINT '=== Deleting 20 tables (51-70) ==='
GO

-- Step 1: Delete InvoiceItem records
DELETE FROM InvoiceItem 
WHERE dining_table_id IN (SELECT dining_table_id FROM DiningTable WHERE dining_table_id > 50);
GO

-- Step 2: Delete InvoiceDiningTable records
DELETE FROM InvoiceDiningTable 
WHERE dining_table_id IN (SELECT dining_table_id FROM DiningTable WHERE dining_table_id > 50);
GO

-- Step 3: Delete tables 51-70
DELETE FROM DiningTable WHERE dining_table_id > 50;
GO

-- Step 4: Verify
SELECT COUNT(*) as remaining_tables FROM DiningTable;
GO

PRINT '=== DONE ==='
GO
