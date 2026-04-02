-- TEST WALK-IN TABLE SUGGESTION LOGIC
-- Verify that backend NEVER suggests tables from different floors

-- Test Case 1: 19 guests
-- Expected: Should suggest tables from SAME FLOOR only
-- A9 (10) + E1 (10) = WRONG (different floors)
-- Correct options:
--   - A3+A4+A6 (8+8+6=22) - Tầng 1, Khu A
--   - A4+A9 (8+10=18) - NOT enough
--   - B1+B2+B8 (8+6+6=20) - Tầng 1, Khu B
--   - E1+E2 (10+10=20) - Tầng 2, Khu E

SELECT 
    dining_table_id,
    table_name,
    seating_capacity,
    area,
    floor,
    table_status
FROM DiningTable
WHERE table_status = 'AVAILABLE'
    AND floor IS NOT NULL
    AND area IS NOT NULL
ORDER BY floor, area, dining_table_id;

-- Check available tables by floor
SELECT 
    floor,
    area,
    COUNT(*) as table_count,
    SUM(seating_capacity) as total_capacity
FROM DiningTable
WHERE table_status = 'AVAILABLE'
    AND floor IS NOT NULL
    AND area IS NOT NULL
GROUP BY floor, area
ORDER BY floor, area;

-- Verify A9 and E1 are on different floors
SELECT 
    dining_table_id,
    table_name,
    seating_capacity,
    area,
    floor
FROM DiningTable
WHERE dining_table_id IN (9, 40)
ORDER BY dining_table_id;

-- Expected result:
-- ID 9 (A9): floor = 1, area = 'A'
-- ID 40 (E1): floor = 2, area = 'E'
-- These should NEVER be combined!
