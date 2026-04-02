-- =====================================================
-- TEST AREA-BASED SCORING LOGIC
-- =====================================================
-- File này giúp verify thuật toán sắp xếp bàn mới
-- Scoring formula:
-- Score = +2000 (floor=1) + 1000 (floor=2) + 500 (area=A) + 300 (single table)
--         - (max_id - min_id) * 5 - table_count * 30 - |excess_capacity| * 2
-- =====================================================

-- Test Case 1: 10 người
-- Expected: Chọn A9 (10 chỗ, 1 bàn đơn ở tầng 1, khu A)
-- Score = 2000 + 500 + 300 - 0 - 30 - 0 = 2770
PRINT '=== TEST CASE 1: 10 người ===';
SELECT 
    dining_table_id,
    table_name,
    seating_capacity,
    area,
    floor,
    CASE 
        WHEN seating_capacity >= 10 THEN 'MATCH'
        ELSE 'NO'
    END AS single_table_match
FROM DiningTable
WHERE area = 'A' AND floor = 1 AND seating_capacity >= 10
ORDER BY dining_table_id;

-- Test Case 2: 27 người
-- Expected: Ghép 4 bàn trong khu A (A4+A6+A7+A9 = 8+6+6+10 = 30 chỗ)
-- Score = 2000 + 500 - (9-4)*5 - 4*30 - |30-27|*2 = 2500 - 25 - 120 - 6 = 2349
PRINT '';
PRINT '=== TEST CASE 2: 27 người ===';
SELECT 
    dining_table_id,
    table_name,
    seating_capacity,
    area,
    floor
FROM DiningTable
WHERE area = 'A' AND floor = 1
ORDER BY dining_table_id;

PRINT 'Tổng chỗ ngồi khu A:';
SELECT 
    area,
    floor,
    COUNT(*) AS table_count,
    SUM(seating_capacity) AS total_capacity
FROM DiningTable
WHERE area = 'A' AND floor = 1
GROUP BY area, floor;

-- Test Case 3: 50 người
-- Expected: Toàn bộ khu A (52 chỗ) hoặc toàn bộ khu B/C/E (50 chỗ)
-- Khu A score = 2000 + 500 - (9-1)*5 - 9*30 - |52-50|*2 = 2500 - 40 - 270 - 4 = 2186
-- Khu B score = 2000 - (19-10)*5 - 10*30 - 0 = 2000 - 45 - 300 = 1655
-- Khu E score = 1000 - (44-40)*5 - 5*30 - 0 = 1000 - 20 - 150 = 830
PRINT '';
PRINT '=== TEST CASE 3: 50 người ===';
SELECT 
    area,
    floor,
    COUNT(*) AS table_count,
    SUM(seating_capacity) AS total_capacity,
    MIN(dining_table_id) AS min_id,
    MAX(dining_table_id) AS max_id,
    CASE 
        WHEN SUM(seating_capacity) >= 50 THEN 'CAN FIT'
        ELSE 'TOO SMALL'
    END AS fit_status
FROM DiningTable
WHERE area IN ('A', 'B', 'C', 'E')
GROUP BY area, floor
HAVING SUM(seating_capacity) >= 50
ORDER BY floor, area;

-- Test Case 4: Verify area và floor đã được set đúng
PRINT '';
PRINT '=== VERIFY DATABASE STRUCTURE ===';
SELECT 
    area,
    floor,
    COUNT(*) AS table_count,
    SUM(seating_capacity) AS total_capacity
FROM DiningTable
GROUP BY area, floor
ORDER BY floor, area;

-- Test Case 5: Check bàn nào chưa có area/floor
PRINT '';
PRINT '=== TABLES WITHOUT AREA/FLOOR ===';
SELECT 
    dining_table_id,
    table_name,
    seating_capacity,
    area,
    floor
FROM DiningTable
WHERE area IS NULL OR floor IS NULL;

-- Test Case 6: Verify tầng 1 có nhiều bàn nhỏ, tầng 2 có nhiều bàn to
PRINT '';
PRINT '=== FLOOR CAPACITY DISTRIBUTION ===';
SELECT 
    floor,
    seating_capacity,
    COUNT(*) AS table_count
FROM DiningTable
GROUP BY floor, seating_capacity
ORDER BY floor, seating_capacity;

PRINT '';
PRINT '=== TEST COMPLETED ===';
