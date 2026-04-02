-- =====================================================
-- UPDATE KHU A (Tầng 1) - 9 bàn (ID 1-9)
-- =====================================================
USE DATN_SD29_ByHat;
GO

PRINT '=== Updating Khu A (Floor 1) ==='
GO

UPDATE DiningTable SET table_name = N'A1', seating_capacity = 2, area = 'A', floor = 1 WHERE dining_table_id = 1;
UPDATE DiningTable SET table_name = N'A2', seating_capacity = 4, area = 'A', floor = 1 WHERE dining_table_id = 2;
UPDATE DiningTable SET table_name = N'A3', seating_capacity = 8, area = 'A', floor = 1 WHERE dining_table_id = 3;
UPDATE DiningTable SET table_name = N'A4', seating_capacity = 8, area = 'A', floor = 1 WHERE dining_table_id = 4;
UPDATE DiningTable SET table_name = N'A5', seating_capacity = 2, area = 'A', floor = 1 WHERE dining_table_id = 5;
UPDATE DiningTable SET table_name = N'A6', seating_capacity = 6, area = 'A', floor = 1 WHERE dining_table_id = 6;
UPDATE DiningTable SET table_name = N'A7', seating_capacity = 6, area = 'A', floor = 1 WHERE dining_table_id = 7;
UPDATE DiningTable SET table_name = N'A8', seating_capacity = 6, area = 'A', floor = 1 WHERE dining_table_id = 8;
UPDATE DiningTable SET table_name = N'A9', seating_capacity = 10, area = 'A', floor = 1 WHERE dining_table_id = 9;
GO

SELECT dining_table_id, table_name, seating_capacity, area, floor FROM DiningTable WHERE dining_table_id BETWEEN 1 AND 9;
GO

PRINT '=== DONE: Khu A updated ==='
GO
