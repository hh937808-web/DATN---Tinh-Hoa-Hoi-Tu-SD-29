-- =====================================================
-- UPDATE KHU F (Tầng 2) - 9 bàn (ID 45-53)
-- =====================================================
USE DATN_SD29_ByHat;
GO

PRINT '=== Updating Khu F (Floor 2) ==='
GO

UPDATE DiningTable SET table_name = N'F1', seating_capacity = 2, area = 'F', floor = 2 WHERE dining_table_id = 45;
UPDATE DiningTable SET table_name = N'F2', seating_capacity = 2, area = 'F', floor = 2 WHERE dining_table_id = 46;
UPDATE DiningTable SET table_name = N'F3', seating_capacity = 10, area = 'F', floor = 2 WHERE dining_table_id = 47;
UPDATE DiningTable SET table_name = N'F4', seating_capacity = 8, area = 'F', floor = 2 WHERE dining_table_id = 48;
UPDATE DiningTable SET table_name = N'F5', seating_capacity = 2, area = 'F', floor = 2 WHERE dining_table_id = 49;
UPDATE DiningTable SET table_name = N'F6', seating_capacity = 2, area = 'F', floor = 2 WHERE dining_table_id = 50;
UPDATE DiningTable SET table_name = N'F7', seating_capacity = 10, area = 'F', floor = 2 WHERE dining_table_id = 51;
UPDATE DiningTable SET table_name = N'F8', seating_capacity = 6, area = 'F', floor = 2 WHERE dining_table_id = 52;
UPDATE DiningTable SET table_name = N'F9', seating_capacity = 8, area = 'F', floor = 2 WHERE dining_table_id = 53;
GO

SELECT dining_table_id, table_name, seating_capacity, area, floor FROM DiningTable WHERE dining_table_id BETWEEN 45 AND 53;
GO

PRINT '=== DONE: Khu F updated ==='
GO
