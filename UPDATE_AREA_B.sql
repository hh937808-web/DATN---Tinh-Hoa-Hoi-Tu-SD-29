-- =====================================================
-- UPDATE KHU B (Tầng 1) - 10 bàn (ID 10-19)
-- =====================================================
USE DATN_SD29_ByHat;
GO

PRINT '=== Updating Khu B (Floor 1) ==='
GO

UPDATE DiningTable SET table_name = N'B1', seating_capacity = 8, area = 'B', floor = 1 WHERE dining_table_id = 10;
UPDATE DiningTable SET table_name = N'B2', seating_capacity = 6, area = 'B', floor = 1 WHERE dining_table_id = 11;
UPDATE DiningTable SET table_name = N'B3', seating_capacity = 4, area = 'B', floor = 1 WHERE dining_table_id = 12;
UPDATE DiningTable SET table_name = N'B4', seating_capacity = 4, area = 'B', floor = 1 WHERE dining_table_id = 13;
UPDATE DiningTable SET table_name = N'B5', seating_capacity = 4, area = 'B', floor = 1 WHERE dining_table_id = 14;
UPDATE DiningTable SET table_name = N'B6', seating_capacity = 4, area = 'B', floor = 1 WHERE dining_table_id = 15;
UPDATE DiningTable SET table_name = N'B7', seating_capacity = 2, area = 'B', floor = 1 WHERE dining_table_id = 16;
UPDATE DiningTable SET table_name = N'B8', seating_capacity = 6, area = 'B', floor = 1 WHERE dining_table_id = 17;
UPDATE DiningTable SET table_name = N'B9', seating_capacity = 6, area = 'B', floor = 1 WHERE dining_table_id = 18;
UPDATE DiningTable SET table_name = N'B10', seating_capacity = 6, area = 'B', floor = 1 WHERE dining_table_id = 19;
GO

SELECT dining_table_id, table_name, seating_capacity, area, floor FROM DiningTable WHERE dining_table_id BETWEEN 10 AND 19;
GO

PRINT '=== DONE: Khu B updated ==='
GO
