# Thuật Toán Sắp Xếp Bàn Thông Minh (Smart Table Selection Algorithm)

## Tổng Quan

Hệ thống sắp xếp bàn tự động dựa trên **Area-Based Scoring System** với các ưu tiên:
1. Tầng 1 trước (khách tiện)
2. Khu trung tâm (Khu A)
3. Bàn gần nhau (ID gần)
4. Ít bàn ghép hơn
5. Không thừa chỗ quá nhiều

---

## Cấu Trúc Nhà Hàng

**Tổng: 53 bàn = 310 chỗ ngồi**

### Tầng 1 (39 bàn = 200 chỗ)
- **Khu A** (Trung tâm/Sảnh chính): 9 bàn = 52 chỗ (ID: 1-9)
- **Khu B**: 10 bàn = 50 chỗ (ID: 10-19)
- **Khu C**: 10 bàn = 50 chỗ (ID: 20-29)
- **Khu D**: 10 bàn = 48 chỗ (ID: 30-39)

### Tầng 2 (14 bàn = 110 chỗ)
- **Khu E**: 5 bàn = 50 chỗ (ID: 40-44) - Nhiều bàn to
- **Khu F**: 9 bàn = 50 chỗ (ID: 45-53) - Có vài bàn nhỏ

**Đặc điểm:**
- Tầng 1: Nhiều bàn nhỏ, ít bàn to
- Tầng 2: Nhiều bàn to, vài bàn nhỏ
- Khu A: Khu trung tâm, ưu tiên cao nhất

---

## Công Thức Tính Điểm (Area Score)

```
Area Score = 
  + 2000 (nếu floor = 1, ưu tiên tầng 1)
  + 1000 (nếu floor = 2)
  + 500 (nếu area = "A", khu trung tâm)
  + 300 (nếu 1 bàn đủ chỗ, không cần ghép)
  - (max_id - min_id) * 5 (phạt khoảng cách ID xa)
  - số_bàn_ghép * 30 (ưu tiên ít bàn hơn)
  - |tổng_chỗ - số_khách| * 2 (phạt thừa chỗ quá nhiều)
```

### Giải thích:
- **+2000**: Tầng 1 được ưu tiên cao (khách tiện, không cần lên cầu thang)
- **+1000**: Tầng 2 điểm thấp hơn
- **+500**: Khu A (trung tâm) được bonus thêm
- **+300**: Không cần ghép bàn (1 bàn đủ) được ưu tiên
- **-5 * khoảng_cách_ID**: Bàn xa nhau bị phạt (A1+A9 tệ hơn A1+A2)
- **-30 * số_bàn**: Ghép nhiều bàn bị phạt (4 bàn tệ hơn 2 bàn)
- **-2 * chỗ_thừa**: Thừa chỗ quá nhiều bị phạt (tối ưu tài nguyên)

---

## Luồng Xử Lý Chính

### Bước 1: Phân Loại Bàn (Fresh vs Recently Used)
```
FOR EACH table IN available_tables:
  IF has_recent_reservation(table, 120 minutes before):
    → recently_used_tables
  ELSE:
    → fresh_tables
```

**Ưu tiên:** Fresh tables trước → Recently used tables sau

---

### Bước 2: Quét Từng Khu Vực (Area Scanning)

```
FOR EACH area IN [A, B, C, D, E, F]:
  best_combo = NULL
  best_score = -1
  
  // Thử 1 bàn đơn
  FOR EACH table IN area_tables:
    IF table.capacity >= guest_count:
      score = calculate_score(table, guest_count)
      IF score > best_score:
        best_combo = [table]
        best_score = score
  
  // Thử ghép 2-4 bàn
  FOR combo_size IN [2, 3, 4]:
    FOR EACH combination IN get_combinations(area_tables, combo_size):
      IF sum(combination.capacity) >= guest_count:
        score = calculate_score(combination, guest_count)
        IF score > best_score:
          best_combo = combination
          best_score = score
  
  area_results[area] = {combo: best_combo, score: best_score}
```

---

### Bước 3: Chọn Khu Tốt Nhất

```
best_area = NULL
best_score = -1

FOR EACH area IN area_results:
  IF area.score > best_score:
    best_area = area
    best_score = area.score

RETURN best_area.combo
```

---

### Bước 4: Fallback (Nếu Không Tìm Được)

**4.1. Thử ghép nhiều khu cùng tầng:**
```
FOR EACH floor IN [1, 2]:
  floor_tables = get_all_tables_in_floor(floor)
  combo = try_select_from_tables(floor_tables, guest_count)
  IF combo NOT empty:
    RETURN combo
```

**4.2. Greedy Algorithm (cuối cùng):**
```
sorted_tables = sort_by_capacity_desc(all_available_tables)
selected = []
total = 0

FOR EACH table IN sorted_tables:
  selected.add(table)
  total += table.capacity
  IF total >= guest_count:
    RETURN selected

RETURN empty (không đủ bàn)
```

---

### Bước 5: Pending Reservation (Khi Không Đủ Bàn)

**Khi nào kích hoạt:**
- Không tìm được bàn đủ điều kiện
- Nhưng vẫn cho phép đặt bàn (không mất khách)

**Xử lý:**
```
IF selected_tables.empty:
  // Tạo reservation không có bàn cụ thể
  invoice.status = "PENDING_CONFIRMATION"
  invoice.tables = [] // Không gán bàn
  invoice.note = "Nhà hàng đang kín, nhân viên sẽ liên hệ xác nhận"
  
  // Gửi thông báo cho nhân viên
  notify_staff(invoice)
  
  RETURN {
    success: true,
    reservationCode: invoice.code,
    status: "PENDING_CONFIRMATION",
    tables: [],
    message: "Đặt bàn thành công! Nhân viên sẽ liên hệ xác nhận."
  }
```

**Nhân viên xử lý:**
1. Xem danh sách "Pending Reservations"
2. Gọi khách xác nhận tình hình
3. Thủ công assign bàn khi có bàn trống
4. Hoặc đề xuất thời gian khác

---

## Ví Dụ Cụ Thể

### Ví dụ 1: 10 người

**Quét Khu A:**
- A9 (10 chỗ) = 2000 + 500 + 300 - 0 - 1*30 - 0 = **2770** ✅

**Quét Khu B:**
- B1 (8 chỗ) không đủ
- B1+B7 (8+2=10) = 2000 - (16-10)*5 - 2*30 - 0 = **1910**

**Kết quả:** Chọn A9 (điểm cao nhất, 1 bàn đơn)

---

### Ví dụ 2: 27 người

**Quét Khu A (Tầng 1):**
- A4+A6+A7+A9 (8+6+6+10=30)
- Score = 2000 + 500 - (9-4)*5 - 4*30 - |30-27|*2
- Score = 2500 - 25 - 120 - 6 = **2349** ✅

**Quét Khu E (Tầng 2):**
- E1+E2+E3 (10+10+10=30)
- Score = 1000 - (42-40)*5 - 3*30 - 6 = **894**

**Kết quả:** Chọn Khu A (tầng 1, khu trung tâm, điểm cao hơn)

---

### Ví dụ 3: 50 người

**Quét Khu A:**
- Toàn bộ khu (52 chỗ)
- Score = 2000 + 500 - (9-1)*5 - 9*30 - 4 = **2186** ✅

**Quét Khu B:**
- Toàn bộ khu (50 chỗ, vừa khít)
- Score = 2000 - (19-10)*5 - 10*30 - 0 = **1655**

**Quét Khu E:**
- Toàn bộ khu (50 chỗ)
- Score = 1000 - (44-40)*5 - 5*30 - 0 = **830**

**Kết quả:** Chọn Khu A (tầng 1, khu trung tâm, dù thừa 2 chỗ)

---

### Ví dụ 4: 20 người, bàn kín

**Tình huống:**
- Khu A: Chỉ trống A1(2) + A5(2) = 4 chỗ (không đủ)
- Khu B: Trống B7(2) + B3(4) = 6 chỗ (không đủ)
- Tất cả khu đều không đủ

**Kết quả:**
```json
{
  "reservationCode": "RSV-xxx",
  "status": "PENDING_CONFIRMATION",
  "tables": [],
  "message": "Đặt bàn thành công! Nhân viên sẽ liên hệ xác nhận trong thời gian sớm nhất."
}
```

---

## Implementation Notes

### Entity Changes
```java
// DiningTable.java
@Column(name = "area", length = 10)
private String area; // A, B, C, D, E, F

@Column(name = "floor")
private Integer floor; // 1, 2
```

### Repository Queries
```java
// DiningTableRepository.java
List<DiningTable> findByAreaAndFloorOrderById(String area, Integer floor);
List<DiningTable> findByFloorOrderByAreaAndId(Integer floor);
```

### Service Methods
```java
// ReservationService.java
private List<DiningTable> selectTablesForGuests(...)
private List<DiningTable> trySelectWithAreaPriority(...)
private int calculateAreaScore(...)
private List<DiningTable> findBestComboInArea(...)
```

### Constants
```java
private static final int MAX_TABLES_TO_COMBINE = 4;
private static final int FLOOR_1_BONUS = 2000;
private static final int FLOOR_2_BONUS = 1000;
private static final int AREA_A_BONUS = 500;
private static final int SINGLE_TABLE_BONUS = 300;
private static final int ID_DISTANCE_PENALTY = 5;
private static final int TABLE_COUNT_PENALTY = 30;
private static final int EXCESS_CAPACITY_PENALTY = 2;
```

---

## Testing Scenarios

1. **1-10 người:** Nên chọn 1 bàn đơn ở tầng 1
2. **11-20 người:** Ghép 2-3 bàn cùng khu, tầng 1
3. **21-50 người:** Ghép nhiều bàn hoặc cả khu, ưu tiên tầng 1
4. **>50 người:** Ghép nhiều khu cùng tầng hoặc khác tầng
5. **Bàn kín:** Pending reservation, chờ nhân viên xác nhận

---

## Lưu Ý Quan Trọng

1. **Bàn gần nhau:** ID càng gần càng tốt (A1+A2 tốt hơn A1+A9)
2. **Ưu tiên tầng 1:** Khách tiện, không cần lên cầu thang
3. **Khu A ưu tiên:** Khu trung tâm/sảnh chính
4. **Tránh thừa chỗ:** Tối ưu tài nguyên nhà hàng
5. **Pending mode:** Không từ chối khách khi hết bàn

---

## Version History

- **v1.0** (2026-04-01): Initial algorithm design
- Area-based scoring system
- Proximity-based table selection
- Pending reservation support
