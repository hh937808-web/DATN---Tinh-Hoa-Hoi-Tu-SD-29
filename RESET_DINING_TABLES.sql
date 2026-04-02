-- =====================================================
-- RESET DINING TABLES - ADD AREA AND FLOOR COLUMNS
-- =====================================================
-- This script will:
-- 1. Delete all existing dining tables
-- 2. Add 'area' and 'floor' columns to DiningTable table
-- 
-- IMPORTANT: Run this script manually in SQL Server Management Studio
-- =====================================================

USE DATN_SD29_ByHat;
GO

PRINT '=== Starting DiningTable Reset ==='
PRINT 'Current database: ' + DB_NAME()
GO

-- Step 1: Delete all existing dining tables
PRINT 'Step 1: Deleting all existing dining tables...'
DELETE FROM DiningTable;
PRINT '-> All dining tables deleted'
GO

-- Step 2: Add 'area' column (khu vực: A, B, C, D, E, F)
PRINT 'Step 2: Adding area column...'
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DiningTable') AND name = 'area')
BEGIN
    ALTER TABLE DiningTable ADD area NVARCHAR(10) NULL;
    PRINT '-> area column added'
END
ELSE
BEGIN
    PRINT '-> area column already exists'
END
GO

-- Step 3: Add 'floor' column (tầng: 1, 2)
PRINT 'Step 3: Adding floor column...'
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DiningTable') AND name = 'floor')
BEGIN
    ALTER TABLE DiningTable ADD floor INT NULL;
    PRINT '-> floor column added'
END
ELSE
BEGIN
    PRINT '-> floor column already exists'
END
GO

-- Step 4: Verify changes
PRINT 'Step 4: Verifying changes...'
SELECT 
    COUNT(*) as total_tables,
    (SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('DiningTable') AND name = 'area') as has_area_column,
    (SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('DiningTable') AND name = 'floor') as has_floor_column
FROM DiningTable;
GO

PRINT '=== DONE! ==='
PRINT 'All dining tables have been deleted'
PRINT 'area and floor columns have been added'
PRINT 'Ready to insert new tables with proper naming (A-1, A-2, B-1, B-2, etc.)'
GO
