# Hướng Dẫn Pending Reservation

## Tổng Quan

Pending Reservation là tính năng cho phép khách đặt bàn ngay cả khi nhà hàng đang kín bàn. Thay vì từ chối khách, hệ thống sẽ tạo đặt bàn với trạng thái "PENDING_CONFIRMATION" và nhân viên sẽ liên hệ xác nhận sau.

---

## Khi Nào Kích Hoạt?

Pending Reservation được kích hoạt khi:
- Khách đặt bàn cho số lượng người lớn (ví dụ: 20 người)
- Tất cả các khu vực đều không đủ bàn trống
- Từng khu có vài bàn trống nhưng không đủ điều kiện ghép

**Ví dụ:**
- Khách đặt 20 người vào 19:00 ngày mai
- Khu A: Chỉ trống A1(2) + A5(2) = 4 chỗ
- Khu B: Trống B7(2) + B3(4) = 6 chỗ
- Khu C: Trống C6(2) + C7(4) = 6 chỗ
- Tổng: Không đủ 20 chỗ → Tạo Pending Reservation

---

## Luồng Xử Lý

### 1. Khách Đặt Bàn (Frontend)
```
Khách điền form đặt bàn:
- Số người: 20
- Thời gian: 19:00 ngày mai
- Ghi chú: "Sinh nhật công ty"

→ Submit form
```

### 2. Backend Xử Lý
```java
// ReservationService.java
List<DiningTable> selectedTables = selectTablesForGuests(...);

if (selectedTables.isEmpty()) {
    // Không tìm được bàn → Tạo Pending Reservation
    invoice.setInvoiceStatus("PENDING_CONFIRMATION");
    invoice.setReservationNote(
        (request.getNote() != null ? request.getNote() + " | " : "") +
        "Nhà hàng đang kín, nhân viên sẽ liên hệ xác nhận"
    );
    // Không gán bàn cụ thể
}
```

### 3. Response Trả Về
```json
{
  "reservationCode": "RSV-1234567890",
  "status": "PENDING_CONFIRMATION",
  "tables": [],
  "message": "Đặt bàn thành công! Nhân viên sẽ liên hệ xác nhận trong thời gian sớm nhất.",
  "guestCount": 20,
  "reservedAt": "2026-04-02T19:00:00"
}
```

### 4. Nhân viên Xử Lý

**Bước 1: Xem danh sách Pending Reservations**
```sql
SELECT 
    invoice_id,
    reservation_code,
    reserved_at,
    guest_count,
    invoice_status,
    reservation_note
FROM Invoice
WHERE invoice_status = 'PENDING_CONFIRMATION'
ORDER BY reserved_at;
```

**Bước 2: Gọi khách xác nhận**
- Kiểm tra tình hình bàn thực tế
- Gọi khách báo cáo:
  - "Xin lỗi quý khách, hiện tại nhà hàng đang kín. Quý khách có thể đổi sang 20:00 được không?"
  - Hoặc: "Chúng tôi có thể sắp xếp bàn cho quý khách, nhưng có thể phải ngồi 2 khu khác nhau"

**Bước 3: Cập nhật trạng thái**

Nếu khách đồng ý:
```sql
-- Assign bàn thủ công
UPDATE Invoice 
SET invoice_status = 'RESERVED'
WHERE invoice_id = [ID];

-- Gán bàn cụ thể
INSERT INTO InvoiceDiningTable (invoice_id, dining_table_id)
VALUES ([invoice_id], [table_id_1]), ([invoice_id], [table_id_2]);
```

Nếu khách hủy:
```sql
UPDATE Invoice 
SET invoice_status = 'CANCELLED'
WHERE invoice_id = [ID];
```

---

## Trạng Thái Invoice

| Trạng Thái | Mô Tả | Có Bàn? |
|------------|-------|---------|
| PENDING_CONFIRMATION | Chờ nhân viên xác nhận | Không |
| RESERVED | Đã xác nhận, có bàn cụ thể | Có |
| IN_PROGRESS | Khách đã check-in | Có |
| PAID | Đã thanh toán | Có |
| CANCELLED | Đã hủy | Không |
| NO_SHOW | Khách không đến | Có |

---

## Frontend Display

### Màn Hình Khách
```
✅ Đặt bàn thành công!

Mã đặt bàn: RSV-1234567890
Trạng thái: Chờ xác nhận
Số người: 20
Thời gian: 19:00 - 02/04/2026

⚠️ Lưu ý: Nhà hàng đang kín, nhân viên sẽ liên hệ xác nhận trong thời gian sớm nhất.

Bàn: Chưa được sắp xếp
```

### Màn Hình Nhân Viên
```
📋 Danh Sách Đặt Bàn Chờ Xác Nhận

┌─────────────────────────────────────────────────┐
│ RSV-1234567890 | 20 người | 19:00 - 02/04/2026 │
│ Ghi chú: Sinh nhật công ty                     │
│ Trạng thái: PENDING_CONFIRMATION               │
│ [Gọi Khách] [Assign Bàn] [Hủy]                │
└─────────────────────────────────────────────────┘
```

---

## API Endpoints

### 1. Tạo Pending Reservation
```
POST /api/reservations
Body: {
  "reservedAt": "2026-04-02T19:00:00",
  "guestCount": 20,
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0901234567",
  "promotionType": "Ưu đãi sinh nhật 10% tổng hóa đơn",
  "note": "Sinh nhật công ty"
}

Response: {
  "reservationCode": "RSV-1234567890",
  "status": "PENDING_CONFIRMATION",
  "tables": [],
  "message": "Đặt bàn thành công! Nhân viên sẽ liên hệ xác nhận."
}
```

### 2. Lấy Danh Sách Pending Reservations
```
GET /api/staff/reservations/pending

Response: [
  {
    "invoiceId": 123,
    "reservationCode": "RSV-1234567890",
    "reservedAt": "2026-04-02T19:00:00",
    "guestCount": 20,
    "fullName": "Nguyễn Văn A",
    "phoneNumber": "0901234567",
    "status": "PENDING_CONFIRMATION",
    "note": "Sinh nhật công ty | Nhà hàng đang kín, nhân viên sẽ liên hệ xác nhận"
  }
]
```

### 3. Assign Bàn Cho Pending Reservation
```
POST /api/staff/reservations/{invoiceId}/assign-tables
Body: {
  "tableIds": [1, 2, 3, 4]
}

Response: {
  "success": true,
  "message": "Đã assign bàn thành công",
  "status": "RESERVED"
}
```

---

## Database Schema

### Invoice Table
```sql
CREATE TABLE Invoice (
    invoice_id INT PRIMARY KEY,
    reservation_code VARCHAR(50),
    invoice_status VARCHAR(50), -- PENDING_CONFIRMATION, RESERVED, etc.
    reserved_at DATETIME,
    guest_count INT,
    reservation_note NVARCHAR(500),
    ...
);
```

### InvoiceDiningTable Table
```sql
CREATE TABLE InvoiceDiningTable (
    invoice_dining_table_id INT PRIMARY KEY,
    invoice_id INT,
    dining_table_id INT,
    FOREIGN KEY (invoice_id) REFERENCES Invoice(invoice_id),
    FOREIGN KEY (dining_table_id) REFERENCES DiningTable(dining_table_id)
);
```

**Lưu ý:** Pending Reservation sẽ KHÔNG có record trong `InvoiceDiningTable` cho đến khi nhân viên assign bàn.

---

## Testing Scenarios

### Scenario 1: Tạo Pending Reservation
```
1. Khách đặt 20 người vào 19:00 ngày mai
2. Hệ thống check: Không đủ bàn
3. Tạo invoice với status = "PENDING_CONFIRMATION"
4. Không gán bàn (InvoiceDiningTable rỗng)
5. Trả về response với tables = []
```

### Scenario 2: Nhân Viên Assign Bàn
```
1. Nhân viên xem danh sách pending
2. Gọi khách xác nhận
3. Chọn bàn thủ công: A1, A2, A3, A4
4. Click "Assign Bàn"
5. Hệ thống:
   - Update status → "RESERVED"
   - Insert vào InvoiceDiningTable
   - Broadcast WebSocket
```

### Scenario 3: Khách Hủy
```
1. Nhân viên gọi khách
2. Khách không đồng ý thời gian khác
3. Nhân viên click "Hủy"
4. Hệ thống update status → "CANCELLED"
```

---

## Lợi Ích

1. **Không mất khách:** Vẫn cho phép đặt bàn khi hết chỗ
2. **Linh hoạt:** Nhân viên có thể sắp xếp thủ công
3. **Trải nghiệm tốt:** Khách không bị từ chối ngay lập tức
4. **Tối ưu doanh thu:** Có cơ hội phục vụ thêm khách

---

## Lưu Ý Quan Trọng

1. **Không tự động assign bàn:** Pending reservation KHÔNG có bàn cụ thể
2. **Nhân viên phải xử lý:** Cần gọi khách xác nhận trong vòng 24h
3. **Không hiển thị trên sơ đồ bàn:** Vì chưa có bàn cụ thể
4. **Có thể hủy bất cứ lúc nào:** Không ảnh hưởng đến bàn khác

---

## Version History

- **v1.0** (2026-04-01): Initial implementation
  - Pending reservation support
  - Staff manual assignment
  - Status tracking

