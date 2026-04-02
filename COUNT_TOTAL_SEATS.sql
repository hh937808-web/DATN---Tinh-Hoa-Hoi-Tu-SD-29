-- Đếm tổng số chỗ ngồi của tất cả bàn
SELECT 
    COUNT(*) AS total_tables,
    SUM(seating_capacity) AS total_seats
FROM DiningTable;

-- Đếm chi tiết theo khu vực
SELECT 
    area,
    floor,
    COUNT(*) AS table_count,
    SUM(seating_capacity) AS seats_in_area
FROM DiningTable
GROUP BY area, floor
ORDER BY floor, area;

-- Đếm theo loại bàn (số chỗ ngồi)
SELECT 
    seating_capacity AS table_type,
    COUNT(*) AS table_count,
    SUM(seating_capacity) AS total_seats_for_type
FROM DiningTable
GROUP BY seating_capacity
ORDER BY seating_capacity;
