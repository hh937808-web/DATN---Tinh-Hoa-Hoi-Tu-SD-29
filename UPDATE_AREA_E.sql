-- =====================================================
-- UPDATE KHU E (Tầng 2) - 5 bàn (ID 40-44)
-- =====================================================
USE DATN_SD29_ByHat;
GO

PRINT '=== Updating Khu E (Floor 2) ==='
GO

UPDATE DiningTable SET table_name = N'E1', seating_capacity = 10, area = 'E', floor = 2 WHERE dining_table_id = 40;
UPDATE DiningTable SET table_name = N'E2', seating_capacity = 10, area = 'E', floor = 2 WHERE dining_table_id = 41;
UPDATE DiningTable SET table_name = N'E3', seating_capacity = 10, area = 'E', floor = 2 WHERE dining_table_id = 42;
UPDATE DiningTable SET table_name = N'E4', seating_capacity = 10, area = 'E', floor = 2 WHERE dining_table_id = 43;
UPDATE DiningTable SET table_name = N'E5', seating_capacity = 10, area = 'E', floor = 2 WHERE dining_table_id = 44;
GO

SELECT dining_table_id, table_name, seating_capacity, area, floor FROM DiningTable WHERE dining_table_id BETWEEN 40 AND 44;
GO

PRINT '=== DONE: Khu E updated ==='
GO
