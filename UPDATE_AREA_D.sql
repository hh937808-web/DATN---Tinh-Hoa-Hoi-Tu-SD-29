-- =====================================================
-- UPDATE KHU D (Tầng 1) - 10 bàn (ID 30-39)
-- =====================================================
USE DATN_SD29_ByHat;
GO

PRINT '=== Updating Khu D (Floor 1) ==='
GO

UPDATE DiningTable SET table_name = N'D1', seating_capacity = 8, area = 'D', floor = 1 WHERE dining_table_id = 30;
UPDATE DiningTable SET table_name = N'D2', seating_capacity = 2, area = 'D', floor = 1 WHERE dining_table_id = 31;
UPDATE DiningTable SET table_name = N'D3', seating_capacity = 4, area = 'D', floor = 1 WHERE dining_table_id = 32;
UPDATE DiningTable SET table_name = N'D4', seating_capacity = 6, area = 'D', floor = 1 WHERE dining_table_id = 33;
UPDATE DiningTable SET table_name = N'D5', seating_capacity = 2, area = 'D', floor = 1 WHERE dining_table_id = 34;
UPDATE DiningTable SET table_name = N'D6', seating_capacity = 4, area = 'D', floor = 1 WHERE dining_table_id = 35;
UPDATE DiningTable SET table_name = N'D7', seating_capacity = 4, area = 'D', floor = 1 WHERE dining_table_id = 36;
UPDATE DiningTable SET table_name = N'D8', seating_capacity = 6, area = 'D', floor = 1 WHERE dining_table_id = 37;
UPDATE DiningTable SET table_name = N'D9', seating_capacity = 6, area = 'D', floor = 1 WHERE dining_table_id = 38;
UPDATE DiningTable SET table_name = N'D10', seating_capacity = 6, area = 'D', floor = 1 WHERE dining_table_id = 39;
GO

SELECT dining_table_id, table_name, seating_capacity, area, floor FROM DiningTable WHERE dining_table_id BETWEEN 30 AND 39;
GO

PRINT '=== DONE: Khu D updated ==='
GO
