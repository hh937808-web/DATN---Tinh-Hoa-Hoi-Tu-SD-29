# MongoDB Setup cho Audit Log

## Khởi động MongoDB bằng Docker

```bash
cd DATN---Tinh-Hoa-Hoi-Tu-SD-29
docker-compose -f docker-compose-mongodb.yml up -d
```

## Kiểm tra MongoDB đang chạy

```bash
docker ps
```

Bạn sẽ thấy 2 containers:
- `audit-mongodb` - MongoDB server (port 27017)
- `mongo-express` - Web UI để xem dữ liệu (port 8081)

## Truy cập Mongo Express (Web UI)

Mở trình duyệt: http://localhost:8081

Bạn sẽ thấy database `audit_db` và collection `audit_logs`

## Dừng MongoDB

```bash
docker-compose -f docker-compose-mongodb.yml down
```

## Xóa dữ liệu MongoDB (nếu cần reset)

```bash
docker-compose -f docker-compose-mongodb.yml down -v
```

## Kết nối từ Spring Boot

Spring Boot sẽ tự động kết nối đến MongoDB qua biến môi trường:
```
MONGODB_URI=mongodb://localhost:27017/audit_db
```

Đã được cấu hình trong file `.env`
