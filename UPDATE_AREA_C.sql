-- =====================================================
-- UPDATE KHU C (Tầng 1) - 10 bàn (ID 20-29)
-- =====================================================
USE DATN_SD29_ByHat;
GO

PRINT '=== Updating Khu C (Floor 1) ==='
GO

UPDATE DiningTable SET table_name = N'C1', seating_capacity = 6, area = 'C', floor = 1 WHERE dining_table_id = 20;
UPDATE DiningTable SET table_name = N'C2', seating_capacity = 6, area = 'C', floor = 1 WHERE dining_table_id = 21;
UPDATE DiningTable SET table_name = N'C3', seating_capacity = 6, area = 'C', floor = 1 WHERE dining_table_id = 22;
UPDATE DiningTable SET table_name = N'C4', seating_capacity = 4, area = 'C', floor = 1 WHERE dining_table_id = 23;
UPDATE DiningTable SET table_name = N'C5', seating_capacity = 4, area = 'C', floor = 1 WHERE dining_table_id = 24;
UPDATE DiningTable SET table_name = N'C6', seating_capacity = 2, area = 'C', floor = 1 WHERE dining_table_id = 25;
UPDATE DiningTable SET table_name = N'C7', seating_capacity = 4, area = 'C', floor = 1 WHERE dining_table_id = 26;
UPDATE DiningTable SET table_name = N'C8', seating_capacity = 4, area = 'C', floor = 1 WHERE dining_table_id = 27;
UPDATE DiningTable SET table_name = N'C9', seating_capacity = 6, area = 'C', floor = 1 WHERE dining_table_id = 28;
UPDATE DiningTable SET table_name = N'C10', seating_capacity = 8, area = 'C', floor = 1 WHERE dining_table_id = 29;
GO

SELECT dining_table_id, table_name, seating_capacity, area, floor FROM DiningTable WHERE dining_table_id BETWEEN 20 AND 29;
GO

PRINT '=== DONE: Khu C updated ==='
GO
