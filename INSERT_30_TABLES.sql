-- =====================================================
-- INSERT 30 TABLES (21-50) - Temporary names
-- =====================================================
USE DATN_SD29_ByHat;
GO

PRINT '=== Inserting 30 tables (21-50) ==='
GO

SET IDENTITY_INSERT DiningTable ON;
GO

INSERT INTO DiningTable (dining_table_id, table_name, seating_capacity, table_status, created_at)
VALUES
(21, N'Bàn 21', 4, N'AVAILABLE', GETDATE()),
(22, N'Bàn 22', 4, N'AVAILABLE', GETDATE()),
(23, N'Bàn 23', 4, N'AVAILABLE', GETDATE()),
(24, N'Bàn 24', 4, N'AVAILABLE', GETDATE()),
(25, N'Bàn 25', 4, N'AVAILABLE', GETDATE()),
(26, N'Bàn 26', 4, N'AVAILABLE', GETDATE()),
(27, N'Bàn 27', 4, N'AVAILABLE', GETDATE()),
(28, N'Bàn 28', 4, N'AVAILABLE', GETDATE()),
(29, N'Bàn 29', 4, N'AVAILABLE', GETDATE()),
(30, N'Bàn 30', 4, N'AVAILABLE', GETDATE()),
(31, N'Bàn 31', 6, N'AVAILABLE', GETDATE()),
(32, N'Bàn 32', 6, N'AVAILABLE', GETDATE()),
(33, N'Bàn 33', 6, N'AVAILABLE', GETDATE()),
(34, N'Bàn 34', 6, N'AVAILABLE', GETDATE()),
(35, N'Bàn 35', 6, N'AVAILABLE', GETDATE()),
(36, N'Bàn 36', 6, N'AVAILABLE', GETDATE()),
(37, N'Bàn 37', 6, N'AVAILABLE', GETDATE()),
(38, N'Bàn 38', 6, N'AVAILABLE', GETDATE()),
(39, N'Bàn 39', 6, N'AVAILABLE', GETDATE()),
(40, N'Bàn 40', 6, N'AVAILABLE', GETDATE()),
(41, N'Bàn 41', 8, N'AVAILABLE', GETDATE()),
(42, N'Bàn 42', 8, N'AVAILABLE', GETDATE()),
(43, N'Bàn 43', 8, N'AVAILABLE', GETDATE()),
(44, N'Bàn 44', 8, N'AVAILABLE', GETDATE()),
(45, N'Bàn 45', 10, N'AVAILABLE', GETDATE()),
(46, N'Bàn 46', 10, N'AVAILABLE', GETDATE()),
(47, N'Bàn 47', 10, N'AVAILABLE', GETDATE()),
(48, N'Bàn 48', 10, N'AVAILABLE', GETDATE()),
(49, N'Bàn 49', 10, N'AVAILABLE', GETDATE()),
(50, N'Bàn 50', 10, N'AVAILABLE', GETDATE());
GO

SET IDENTITY_INSERT DiningTable OFF;
GO

SELECT COUNT(*) as total_tables FROM DiningTable;
GO

PRINT '=== DONE: 50 tables total ==='
GO

-- =====================================================
-- INSERT 3 MORE TABLES (51-53)
-- =====================================================
PRINT '=== Inserting 3 more tables (51-53) ==='
GO

SET IDENTITY_INSERT DiningTable ON;
GO

INSERT INTO DiningTable (dining_table_id, table_name, seating_capacity, table_status, created_at)
VALUES
(51, N'Bàn 51', 4, N'AVAILABLE', GETDATE()),
(52, N'Bàn 52', 4, N'AVAILABLE', GETDATE()),
(53, N'Bàn 53', 4, N'AVAILABLE', GETDATE());
GO

SET IDENTITY_INSERT DiningTable OFF;
GO

SELECT COUNT(*) as total_tables FROM DiningTable;
GO

PRINT '=== DONE: 53 tables total ==='
GO
