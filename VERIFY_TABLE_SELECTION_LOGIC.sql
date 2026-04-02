-- =====================================================
-- VERIFY TABLE SELECTION LOGIC
-- Kiểm tra logic xếp bàn theo khu vực và tầng
-- =====================================================

-- 1. Kiểm tra tất cả bàn có area và floor
SELECT 
    dining_table_id,
    table_name,
    seating_capacity,
    area,
    floor,
    table_status
FROM DiningTable
ORDER BY floor, area, dining_table_id;

-- 2. Thống kê bàn theo khu vực
SELECT 
    area,
    floor,
    COUNT(*) as total_tables,
    SUM(seating_capacity) as total_capacity,
    STRING_AGG(table_name, ', ') as table_names
FROM DiningTable
GROUP BY area, floor
ORDER BY floor, area;

-- 3. Kiểm tra bàn trống (AVAILABLE) theo khu vực
SELECT 
    area,
    floor,
    COUNT(*) as available_tables,
    SUM(seating_capacity) as available_capacity,
    STRING_AGG(table_name, ', ') as available_table_names
FROM DiningTable
WHERE table_status = 'AVAILABLE' OR table_status IS NULL
GROUP BY area, floor
ORDER BY floor, area;

-- 4. Kiểm tra bàn gần nhau trong cùng khu (theo ID tăng dần)
-- Ví dụ: Khu A - A1, A2, A3 nên có ID liên tiếp
SELECT 
    area,
    floor,
    table_name,
    dining_table_id,
    seating_capacity,
    LAG(dining_table_id) OVER (PARTITION BY area, floor ORDER BY dining_table_id) as prev_id,
    dining_table_id - LAG(dining_table_id) OVER (PARTITION BY area, floor ORDER BY dining_table_id) as id_gap
FROM DiningTable
ORDER BY floor, area, dining_table_id;

-- 5. Tìm các khu có thể ghép bàn (2-4 bàn cùng khu)
-- Ví dụ: Khu A có thể ghép A1+A2 cho 6 người
SELECT 
    area,
    floor,
    COUNT(*) as table_count,
    SUM(seating_capacity) as total_capacity,
    STRING_AGG(CONCAT(table_name, '(', seating_capacity, ')'), ' + ') as combination
FROM DiningTable
WHERE table_status = 'AVAILABLE' OR table_status IS NULL
GROUP BY area, floor
HAVING COUNT(*) >= 2 AND COUNT(*) <= 4
ORDER BY floor, area;

-- 6. Kiểm tra bàn NULL area hoặc floor (cần fix)
SELECT 
    dining_table_id,
    table_name,
    seating_capacity,
    area,
    floor,
    table_status
FROM DiningTable
WHERE area IS NULL OR floor IS NULL;

PRINT '=== VERIFICATION COMPLETE ==='
PRINT 'Check results above to ensure:'
PRINT '1. All tables have area and floor assigned'
PRINT '2. Tables in same area have consecutive IDs (A1, A2, A3...)'
PRINT '3. Each area has reasonable table distribution'
