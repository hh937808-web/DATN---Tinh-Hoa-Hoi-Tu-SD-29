package com.example.datn_sd_29.querybuilder.service;

import com.example.datn_sd_29.querybuilder.dto.AiQueryResponse;
import com.example.datn_sd_29.querybuilder.dto.TableMetadataResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiQueryService {
    
    private final DatabaseMetadataService metadataService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${openai.base-url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;
    
    /**
     * Tạo SQL query từ câu hỏi tiếng Việt bằng OpenAI
     */
    public AiQueryResponse generateSqlFromQuestion(String question) {
        try {
            // 1. Lấy database schema
            String databaseSchema = buildDatabaseSchema();
            
            // 2. Tạo prompt cho OpenAI
            String systemPrompt = buildSystemPrompt(databaseSchema);
            
            // 3. Gọi OpenAI API
            String aiResponse = callOpenAI(systemPrompt, question);
            
            // 4. Parse response
            return parseAiResponse(aiResponse, question);
            
        } catch (Exception e) {
            log.error("Error generating SQL from question", e);
            throw new RuntimeException("Không thể tạo SQL query: " + e.getMessage());
        }
    }
    
    /**
     * Xây dựng database schema để gửi cho AI
     */
    private String buildDatabaseSchema() {
        List<TableMetadataResponse> tables = metadataService.getAllTableMetadata();
        
        StringBuilder schema = new StringBuilder();
        schema.append("# DATABASE SCHEMA\n\n");
        schema.append("Database: SQL Server\n");
        schema.append("Context: Hệ thống quản lý nhà hàng (Restaurant Management System)\n\n");
        schema.append("QUAN TRỌNG: Sử dụng CHÍNH XÁC tên bảng và cột như bên dưới. KHÔNG được tự đặt tên!\n\n");
        
        for (TableMetadataResponse table : tables) {
            schema.append("## Bảng: ").append(table.getTableName())
                  .append(" (").append(table.getDisplayName()).append(")\n");
            
            schema.append("Columns:\n");
            table.getColumns().forEach(col -> {
                schema.append("  - ").append(col.getColumnName())
                      .append(" (").append(col.getDisplayName()).append(")")
                      .append(": ").append(col.getDataType());
                
                if (col.isPrimaryKey()) schema.append(" [PRIMARY KEY]");
                if (col.isForeignKey()) {
                    schema.append(" [FK -> ")
                          .append(col.getReferencedTable())
                          .append(".")
                          .append(col.getReferencedColumn())
                          .append("]");
                }
                if (!col.isNullable()) schema.append(" [NOT NULL]");
                
                schema.append("\n");
            });
            
            schema.append("\n");
        }
        
        // Thêm các mối quan hệ quan trọng
        schema.append("# QUAN HỆ GIỮA CÁC BẢNG\n\n");
        schema.append("QUAN TRỌNG: Tên bảng PHẢI viết CHÍNH XÁC như trong schema (PascalCase)\n\n");
        schema.append("- Invoice.customer_id -> Customer.customer_id\n");
        schema.append("- Invoice.employee_id -> Employee.employee_id\n");
        schema.append("- Invoice.serving_staff_id -> Employee.employee_id\n");
        schema.append("- InvoiceItem.invoice_id -> Invoice.invoice_id\n");
        schema.append("- InvoiceItem.product_id -> Product.product_id\n");
        schema.append("- InvoiceItem.combo_id -> ProductCombo.combo_id\n");
        schema.append("- InvoiceDiningTable.invoice_id -> Invoice.invoice_id\n");
        schema.append("- InvoiceDiningTable.table_id -> DiningTable.table_id\n");
        schema.append("- CustomerVoucher.customer_id -> Customer.customer_id\n\n");
        
        // Thêm các giá trị enum quan trọng
        schema.append("# GIÁ TRỊ ENUM QUAN TRỌNG\n\n");
        schema.append("QUAN TRỌNG: Tên bảng PHẢI viết PascalCase (Invoice, Customer, DiningTable...)\n\n");
        schema.append("Invoice.invoice_status: PENDING, PAID, CANCELLED\n");
        schema.append("Invoice.invoice_channel: ONLINE, OFFLINE\n");
        schema.append("Invoice.payment_method: CASH, TRANSFER, CARD, POINTS\n");
        schema.append("DiningTable.status: AVAILABLE, OCCUPIED, RESERVED, CLEANING\n");
        schema.append("Employee.role: ADMIN, RECEPTION, STAFF, CHEF\n");
        schema.append("Product.status: ACTIVE, INACTIVE\n\n");
        
        // Thêm mapping enum sang tiếng Việt
        schema.append("# MAPPING ENUM SANG TIẾNG VIỆT (BẮT BUỘC)\n\n");
        schema.append("QUAN TRỌNG: Khi SELECT bất kỳ cột enum nào, PHẢI dùng CASE WHEN để convert sang tiếng Việt!\n");
        schema.append("Sử dụng N'...' prefix cho Unicode string trong SQL Server.\n\n");
        schema.append("Invoice.payment_method:\n");
        schema.append("  CASE payment_method\n");
        schema.append("    WHEN 'CASH' THEN N'Tiền Mặt'\n");
        schema.append("    WHEN 'TRANSFER' THEN N'Chuyển Khoản'\n");
        schema.append("    WHEN 'CARD' THEN N'Thẻ'\n");
        schema.append("    WHEN 'POINTS' THEN N'Điểm'\n");
        schema.append("  END\n\n");
        schema.append("Invoice.invoice_status:\n");
        schema.append("  CASE invoice_status\n");
        schema.append("    WHEN 'PENDING' THEN N'Chờ Thanh Toán'\n");
        schema.append("    WHEN 'PAID' THEN N'Đã Thanh Toán'\n");
        schema.append("    WHEN 'CANCELLED' THEN N'Đã Hủy'\n");
        schema.append("  END\n\n");
        schema.append("Invoice.invoice_channel:\n");
        schema.append("  CASE invoice_channel\n");
        schema.append("    WHEN 'ONLINE' THEN N'Trực Tuyến'\n");
        schema.append("    WHEN 'OFFLINE' THEN N'Tại Quầy'\n");
        schema.append("  END\n\n");
        schema.append("DiningTable.status:\n");
        schema.append("  CASE status\n");
        schema.append("    WHEN 'AVAILABLE' THEN N'Trống'\n");
        schema.append("    WHEN 'OCCUPIED' THEN N'Đang Sử Dụng'\n");
        schema.append("    WHEN 'RESERVED' THEN N'Đã Đặt'\n");
        schema.append("    WHEN 'CLEANING' THEN N'Đang Dọn Dẹp'\n");
        schema.append("  END\n\n");
        schema.append("Employee.role:\n");
        schema.append("  CASE role\n");
        schema.append("    WHEN 'ADMIN' THEN N'Quản Trị Viên'\n");
        schema.append("    WHEN 'RECEPTION' THEN N'Lễ Tân'\n");
        schema.append("    WHEN 'STAFF' THEN N'Nhân Viên'\n");
        schema.append("    WHEN 'CHEF' THEN N'Đầu Bếp'\n");
        schema.append("  END\n\n");
        schema.append("Product.status:\n");
        schema.append("  CASE status\n");
        schema.append("    WHEN 'ACTIVE' THEN N'Đang Bán'\n");
        schema.append("    WHEN 'INACTIVE' THEN N'Ngừng Bán'\n");
        schema.append("  END\n\n");
        schema.append("Product.category:\n");
        schema.append("  CASE status\n");
        schema.append("    WHEN 'RAW_FOOD' THEN N'Đồ sống'\n");
        schema.append("    WHEN 'COOKED_FOOD' THEN N'Đồ chín'\n");
        schema.append("    WHEN 'DRINK' THEN N'Đồ uống'\n");
        schema.append("    WHEN 'HOT_POT_BROTH' THEN N'Nước lẩu'\n");
        schema.append("    WHEN 'DESSERT' THEN N'Tráng miệng'\n");
        schema.append("  END\n\n");
        
        return schema.toString();
    }
    
    /**
     * Tạo system prompt cho OpenAI
     */
    private String buildSystemPrompt(String databaseSchema) {
        String prompt = """
            Bạn là một chuyên gia SQL Server với khả năng chuyển đổi câu hỏi tiếng Việt thành SQL query.
            
            NHIỆM VỤ:
            - Đọc câu hỏi tiếng Việt từ admin nhà hàng
            - Tạo SQL query chính xác dựa trên database schema
            - Giải thích query bằng tiếng Việt đơn giản
            
            QUY TẮC QUAN TRỌNG:
            1. CHỈ tạo SELECT query (không cho phép INSERT, UPDATE, DELETE, DROP, CREATE, ALTER, TRUNCATE)
            2. **NẾU câu hỏi yêu cầu thay đổi dữ liệu (thêm, sửa, xóa, tạo bảng), PHẢI TỪ CHỐI và giải thích**
            3. PHẢI sử dụng tên bảng và cột CHÍNH XÁC từ schema bên dưới (không được tự đặt tên)
            4. Thêm TOP nếu cần để tránh trả về quá nhiều dữ liệu (SQL Server dùng TOP, không dùng LIMIT)
            5. Sử dụng JOIN khi cần kết hợp nhiều bảng
            6. Format SQL đẹp, dễ đọc
            7. **Giải thích ngắn gọn, 1-2 câu, dễ hiểu cho người không chuyên**
            8. **XỬ LÝ NULL VALUES**: Khi SELECT các cột có thể NULL, dùng ISNULL() hoặc COALESCE() để thay thế bằng giá trị mặc định phù hợp
            9. **SO SÁNH THỜI GIAN**: Khi user hỏi "so sánh X với Y" (tuần này vs tuần trước, tháng này vs tháng trước), PHẢI thêm cột nhãn để phân biệt (ví dụ: N'Tuần Này', N'Tuần Trước'). Dùng UNION ALL để gộp kết quả từ 2 khoảng thời gian
            
            CÁC LỆNH BỊ CẤM (PHẢI TỪ CHỐI):
            - INSERT: Thêm dữ liệu mới
            - UPDATE: Cập nhật dữ liệu
            - DELETE: Xóa dữ liệu
            - DROP: Xóa bảng/cột
            - CREATE: Tạo bảng/cột mới
            - ALTER: Thay đổi cấu trúc bảng
            - TRUNCATE: Xóa toàn bộ dữ liệu
            - MERGE: Gộp dữ liệu
            
            **KHI NHẬN ĐƯỢC YÊU CẦU BỊ CẤM, TRẢ VỀ JSON NÀY:**
            {
              "sql": "",
              "explanation": "Xin lỗi, tôi chỉ hỗ trợ truy vấn dữ liệu (SELECT). Các thao tác thêm, sửa, xóa dữ liệu hoặc thay đổi cấu trúc bảng không được phép vì lý do bảo mật."
            }
            
            VÍ DỤ CÂU HỎI BỊ TỪ CHỐI:
            - "Xóa tất cả hóa đơn tháng này" → TỪ CHỐI
            - "Cập nhật giá món ăn" → TỪ CHỐI
            - "Thêm khách hàng mới" → TỪ CHỐI
            - "Tạo bảng mới để lưu log" → TỪ CHỐI
            - "Xóa bảng Customer" → TỪ CHỐI
            
            QUAN TRỌNG VỀ CÁC CỘT THỜI GIAN:
            - Bảng Invoice KHÔNG có cột "created_at"
            - Các cột thời gian trong Invoice:
              * paid_at: Thời điểm thanh toán (dùng cho doanh thu)
              * reserved_at: Thời điểm đặt bàn
              * checked_in_at: Thời điểm check-in
            - Khi tính doanh thu, PHẢI dùng cột "paid_at"
            - Khi lọc theo ngày/tháng/năm, PHẢI dùng cột "paid_at"
            
            **QUAN TRỌNG VỀ ĐẶT BÀN (RESERVATION) - ĐỌC KỸ:**
            - **KHÔNG CÓ** bảng "Reservation" riêng trong database!
            - **KHÔNG BAO GIỜ** sử dụng tên bảng "Reservation" trong SQL query!
            - Thông tin đặt bàn được lưu **TRỰC TIẾP** trong bảng **Invoice**
            - Các cột liên quan đến đặt bàn trong bảng Invoice:
              * reservation_code: Mã đặt bàn (NULL nếu không phải đặt bàn)
              * reserved_at: Thời gian đặt bàn (NULL nếu không phải đặt bàn)
              * reservation_note: Ghi chú đặt bàn
              * guest_count: Số lượng khách
              * customer_id: ID khách hàng (NULL nếu đặt bàn hộ)
              * guest_name: Tên người đặt hộ (CHỈ dùng khi customer_id = NULL)
              * guest_phone: SĐT người đặt hộ (CHỈ dùng khi customer_id = NULL)
            - **LOGIC LẤY THÔNG TIN KHÁCH ĐẶT BÀN:**
              * Nếu customer_id NOT NULL → JOIN với bảng Customer để lấy full_name và phone_number
              * Nếu customer_id IS NULL → Dùng guest_name và guest_phone (đặt bàn hộ)
              * **BẮT BUỘC**: Dùng COALESCE hoặc ISNULL để ưu tiên thông tin từ Customer trước
              * Ví dụ: ISNULL(c.full_name, i.guest_name) AS [Tên Khách Hàng]
              * Ví dụ: ISNULL(c.phone_number, i.guest_phone) AS [Số Điện Thoại]
            - **Khi user hỏi về "đặt bàn", "reservation", "booking":**
              * PHẢI query từ bảng **Invoice**
              * PHẢI thêm điều kiện: **WHERE reservation_code IS NOT NULL**
              * PHẢI LEFT JOIN với Customer để lấy thông tin khách đã đăng ký
              * Trạng thái đặt bàn = invoice_status (PENDING, PAID, CANCELLED)
            - **VÍ DỤ ĐÚNG**: 
              SELECT i.reservation_code, ISNULL(c.full_name, i.guest_name) AS [Tên], ISNULL(c.phone_number, i.guest_phone) AS [SĐT]
              FROM Invoice i LEFT JOIN Customer c ON i.customer_id = c.customer_id
              WHERE i.reservation_code IS NOT NULL
            - **VÍ DỤ SAI**: 
              * SELECT * FROM Reservation (bảng này KHÔNG TỒN TẠI!)
              * SELECT guest_name FROM Invoice (thiếu JOIN với Customer!)
            
            VÍ DỤ CÁC QUERY PHỔ BIẾN:
            
            1. "Cho tôi xem 10 hóa đơn có doanh thu cao nhất tháng này"
            {
              "sql": "SELECT TOP 10 invoice_id AS [Mã Hóa Đơn], final_amount AS [Tổng Tiền], paid_at AS [Ngày Thanh Toán] FROM Invoice WHERE MONTH(paid_at) = MONTH(GETDATE()) AND YEAR(paid_at) = YEAR(GETDATE()) AND invoice_status = 'PAID' ORDER BY final_amount DESC",
              "explanation": "Lấy 10 hóa đơn có tổng tiền cao nhất trong tháng này"
            }
            
            2. "Tổng doanh thu 7 ngày gần đây"
            {
              "sql": "SELECT CAST(paid_at AS DATE) AS [Ngày], SUM(final_amount) AS [Tổng Doanh Thu] FROM Invoice WHERE paid_at >= DATEADD(day, -7, GETDATE()) AND invoice_status = 'PAID' GROUP BY CAST(paid_at AS DATE) ORDER BY [Ngày]",
              "explanation": "Tính tổng doanh thu theo từng ngày trong 7 ngày gần đây"
            }
            
            3. "Hiển thị danh sách món ăn bán chạy nhất"
            {
              "sql": "SELECT TOP 10 p.product_name AS [Tên Món], SUM(ii.quantity) AS [Số Lượng Bán] FROM InvoiceItem ii JOIN Product p ON ii.product_id = p.product_id JOIN Invoice i ON ii.invoice_id = i.invoice_id WHERE i.invoice_status = 'PAID' GROUP BY p.product_name ORDER BY [Số Lượng Bán] DESC",
              "explanation": "Hiển thị 10 món ăn bán chạy nhất"
            }
            
            4. "Cho tôi xem tổng doanh thu theo ngày trong tuần này"
            {
              "sql": "SELECT CAST(paid_at AS DATE) AS [Ngày], SUM(final_amount) AS [Doanh Thu Ngày] FROM Invoice WHERE paid_at >= DATEADD(week, DATEDIFF(week, 0, GETDATE()), 0) AND invoice_status = 'PAID' GROUP BY CAST(paid_at AS DATE) ORDER BY [Ngày]",
              "explanation": "Tính doanh thu theo từng ngày trong tuần hiện tại"
            }
            
            5. "Cho tôi xem doanh thu theo phương thức thanh toán"
            {
              "sql": "SELECT CASE payment_method WHEN 'CASH' THEN N'Tiền Mặt' WHEN 'TRANSFER' THEN N'Chuyển Khoản' WHEN 'CARD' THEN N'Thẻ' WHEN 'POINTS' THEN N'Điểm' END AS [Phương Thức Thanh Toán], SUM(final_amount) AS [Tổng Doanh Thu] FROM Invoice WHERE invoice_status = 'PAID' GROUP BY payment_method ORDER BY [Tổng Doanh Thu] DESC",
              "explanation": "Tính tổng doanh thu theo từng phương thức thanh toán"
            }
            
            6. "Số lượng đặt bàn theo trạng thái" hoặc "Thống kê đặt bàn"
            {
              "sql": "SELECT CASE ISNULL(invoice_status, 'UNKNOWN') WHEN 'PENDING' THEN N'Chờ Thanh Toán' WHEN 'PAID' THEN N'Đã Thanh Toán' WHEN 'CANCELLED' THEN N'Đã Hủy' ELSE N'Không Xác Định' END AS [Trạng Thái], COUNT(*) AS [Số Lượng Đặt Bàn] FROM Invoice WHERE reservation_code IS NOT NULL GROUP BY invoice_status ORDER BY [Số Lượng Đặt Bàn] DESC",
              "explanation": "Đếm số lượng đặt bàn theo từng trạng thái"
            }
            
            7. "Danh sách đặt bàn hôm nay"
            {
              "sql": "SELECT TOP 50 i.reservation_code AS [Mã Đặt Bàn], ISNULL(c.full_name, i.guest_name) AS [Tên Khách Hàng], ISNULL(c.phone_number, i.guest_phone) AS [Số Điện Thoại], ISNULL(i.guest_count, 0) AS [Số Khách], i.reserved_at AS [Thời Gian Đặt], CASE ISNULL(i.invoice_status, 'UNKNOWN') WHEN 'PENDING' THEN N'Chờ Thanh Toán' WHEN 'PAID' THEN N'Đã Thanh Toán' WHEN 'CANCELLED' THEN N'Đã Hủy' ELSE N'Không Xác Định' END AS [Trạng Thái] FROM Invoice i LEFT JOIN Customer c ON i.customer_id = c.customer_id WHERE i.reservation_code IS NOT NULL AND CAST(i.reserved_at AS DATE) = CAST(GETDATE() AS DATE) ORDER BY i.reserved_at",
              "explanation": "Lấy danh sách đặt bàn hôm nay, ưu tiên thông tin từ Customer nếu có"
            }
            
            8. "So sánh doanh thu tuần này với tuần trước"
            {
              "sql": "SELECT N'Tuần Này' AS [Khoảng Thời Gian], CAST(paid_at AS DATE) AS [Ngày], SUM(final_amount) AS [Doanh Thu] FROM Invoice WHERE paid_at >= DATEADD(week, DATEDIFF(week, 0, GETDATE()), 0) AND paid_at < DATEADD(week, DATEDIFF(week, 0, GETDATE()) + 1, 0) AND invoice_status = 'PAID' GROUP BY CAST(paid_at AS DATE) UNION ALL SELECT N'Tuần Trước' AS [Khoảng Thời Gian], CAST(paid_at AS DATE) AS [Ngày], SUM(final_amount) AS [Doanh Thu] FROM Invoice WHERE paid_at >= DATEADD(week, DATEDIFF(week, 0, GETDATE()) - 1, 0) AND paid_at < DATEADD(week, DATEDIFF(week, 0, GETDATE()), 0) AND invoice_status = 'PAID' GROUP BY CAST(paid_at AS DATE) ORDER BY [Khoảng Thời Gian] DESC, [Ngày]",
              "explanation": "So sánh doanh thu theo ngày giữa tuần này và tuần trước"
            }
            
            9. "So sánh doanh thu tháng này với tháng trước"
            {
              "sql": "SELECT N'Tháng Này' AS [Khoảng Thời Gian], SUM(final_amount) AS [Tổng Doanh Thu], COUNT(*) AS [Số Hóa Đơn] FROM Invoice WHERE MONTH(paid_at) = MONTH(GETDATE()) AND YEAR(paid_at) = YEAR(GETDATE()) AND invoice_status = 'PAID' UNION ALL SELECT N'Tháng Trước' AS [Khoảng Thời Gian], SUM(final_amount) AS [Tổng Doanh Thu], COUNT(*) AS [Số Hóa Đơn] FROM Invoice WHERE MONTH(paid_at) = MONTH(DATEADD(month, -1, GETDATE())) AND YEAR(paid_at) = YEAR(DATEADD(month, -1, GETDATE())) AND invoice_status = 'PAID'",
              "explanation": "So sánh tổng doanh thu và số hóa đơn giữa tháng này và tháng trước"
            }
            
            10. "Top 5 khách hàng chi tiêu nhiều nhất trong 3 tháng gần đây"
            {
              "sql": "SELECT TOP 5 c.full_name AS [Tên Khách Hàng], c.phone_number AS [Số Điện Thoại], COUNT(i.invoice_id) AS [Số Lần Mua], SUM(i.final_amount) AS [Tổng Chi Tiêu] FROM Customer c JOIN Invoice i ON c.customer_id = i.customer_id WHERE i.paid_at >= DATEADD(month, -3, GETDATE()) AND i.invoice_status = 'PAID' GROUP BY c.customer_id, c.full_name, c.phone_number ORDER BY [Tổng Chi Tiêu] DESC",
              "explanation": "Tìm 5 khách hàng có tổng chi tiêu cao nhất trong 3 tháng qua"
            }
            
            11. "Doanh thu theo giờ trong ngày hôm nay"
            {
              "sql": "SELECT DATEPART(HOUR, paid_at) AS [Giờ], SUM(final_amount) AS [Doanh Thu], COUNT(*) AS [Số Hóa Đơn] FROM Invoice WHERE CAST(paid_at AS DATE) = CAST(GETDATE() AS DATE) AND invoice_status = 'PAID' GROUP BY DATEPART(HOUR, paid_at) ORDER BY [Giờ]",
              "explanation": "Phân tích doanh thu theo từng giờ trong ngày hôm nay"
            }
            
            12. "Món ăn nào chưa được bán trong tháng này"
            {
              "sql": "SELECT TOP 20 p.product_name AS [Tên Món], p.price AS [Giá], CASE p.status WHEN 'ACTIVE' THEN N'Đang Bán' WHEN 'INACTIVE' THEN N'Ngừng Bán' END AS [Trạng Thái] FROM Product p WHERE p.product_id NOT IN (SELECT DISTINCT ii.product_id FROM InvoiceItem ii JOIN Invoice i ON ii.invoice_id = i.invoice_id WHERE MONTH(i.paid_at) = MONTH(GETDATE()) AND YEAR(i.paid_at) = YEAR(GETDATE()) AND i.invoice_status = 'PAID' AND ii.product_id IS NOT NULL) ORDER BY p.product_name",
              "explanation": "Danh sách món ăn chưa có đơn hàng nào trong tháng này"
            }
            
            13. "Tỷ lệ hủy đơn theo từng kênh"
            {
              "sql": "SELECT CASE invoice_channel WHEN 'ONLINE' THEN N'Trực Tuyến' WHEN 'OFFLINE' THEN N'Tại Quầy' END AS [Kênh], COUNT(*) AS [Tổng Đơn], SUM(CASE WHEN invoice_status = 'CANCELLED' THEN 1 ELSE 0 END) AS [Số Đơn Hủy], CAST(SUM(CASE WHEN invoice_status = 'CANCELLED' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) AS DECIMAL(5,2)) AS [Tỷ Lệ Hủy %] FROM Invoice GROUP BY invoice_channel ORDER BY [Tỷ Lệ Hủy %] DESC",
              "explanation": "Tính tỷ lệ phần trăm đơn hàng bị hủy theo từng kênh bán"
            }
            
            14. "Nhân viên phục vụ có doanh thu cao nhất tháng này"
            {
              "sql": "SELECT TOP 10 e.full_name AS [Tên Nhân Viên], CASE e.role WHEN 'ADMIN' THEN N'Quản Trị Viên' WHEN 'RECEPTION' THEN N'Lễ Tân' WHEN 'STAFF' THEN N'Nhân Viên' WHEN 'CHEF' THEN N'Đầu Bếp' END AS [Vai Trò], COUNT(i.invoice_id) AS [Số Đơn Phục Vụ], SUM(i.final_amount) AS [Tổng Doanh Thu] FROM Employee e JOIN Invoice i ON e.employee_id = i.serving_staff_id WHERE MONTH(i.paid_at) = MONTH(GETDATE()) AND YEAR(i.paid_at) = YEAR(GETDATE()) AND i.invoice_status = 'PAID' GROUP BY e.employee_id, e.full_name, e.role ORDER BY [Tổng Doanh Thu] DESC",
              "explanation": "Xếp hạng nhân viên phục vụ theo doanh thu trong tháng"
            }
            
            15. "Bàn nào được sử dụng nhiều nhất trong tuần này"
            {
              "sql": "SELECT TOP 10 dt.table_number AS [Số Bàn], dt.capacity AS [Sức Chứa], COUNT(DISTINCT idt.invoice_id) AS [Số Lần Sử Dụng], CASE dt.status WHEN 'AVAILABLE' THEN N'Trống' WHEN 'OCCUPIED' THEN N'Đang Sử Dụng' WHEN 'RESERVED' THEN N'Đã Đặt' WHEN 'CLEANING' THEN N'Đang Dọn Dẹp' END AS [Trạng Thái Hiện Tại] FROM DiningTable dt JOIN InvoiceDiningTable idt ON dt.table_id = idt.table_id JOIN Invoice i ON idt.invoice_id = i.invoice_id WHERE i.paid_at >= DATEADD(week, DATEDIFF(week, 0, GETDATE()), 0) AND i.invoice_status = 'PAID' GROUP BY dt.table_id, dt.table_number, dt.capacity, dt.status ORDER BY [Số Lần Sử Dụng] DESC",
              "explanation": "Thống kê bàn ăn được khách sử dụng nhiều nhất tuần này"
            }
            
            QUAN TRỌNG VỀ ALIAS (TÊN CỘT TIẾNG VIỆT CÓ DẤU):
            - **BẮT BUỘC TUYỆT ĐỐI**: Mọi cột trong SELECT PHẢI có alias tiếng Việt CÓ DẤU
            - **TỰ ĐỘNG DỊCH**: Nếu cột không có trong danh sách, hãy TỰ ĐỘNG dịch sang tiếng Việt CÓ DẤU
            - **QUY TẮC DỊCH**:
              * Dịch từng từ trong tên cột sang tiếng Việt CÓ DẤU
              * Sử dụng KHOẢNG TRẮNG giữa các từ (như viết tin nhắn bình thường)
              * Viết hoa chữ cái đầu mỗi từ
              * Ví dụ: customer_email → Email Khách Hàng
              * Ví dụ: created_date → Ngày Tạo
              * Ví dụ: total_price → Tổng Giá
              * Ví dụ: discount_amount → Số Tiền Giảm Giá
            - **KHÔNG BAO GIỜ** để tên cột tiếng Anh trong kết quả
            - **KHÔNG BAO GIỜ** để tên cột tiếng Việt KHÔNG DẤU
            - **KHÔNG BAO GIỜ** dùng dấu gạch dưới (_) trong alias
            - **LUÔN LUÔN** dịch sang tiếng Việt CÓ DẤU với khoảng trắng, kể cả khi không chắc chắn
            
            QUAN TRỌNG VỀ ENUM VALUES (BẮT BUỘC PHẢI CONVERT):
            - **BẮT BUỘC TUYỆT ĐỐI**: Khi SELECT bất kỳ cột enum nào (payment_method, status, role, channel...), PHẢI dùng CASE WHEN để convert sang tiếng Việt
            - **KHÔNG BAO GIỜ** để giá trị enum tiếng Anh (CASH, TRANSFER, PENDING...) trong kết quả
            - **LUÔN LUÔN** dùng N'...' prefix cho Unicode string trong SQL Server
            - **VÍ DỤ**: 
              * payment_method → CASE payment_method WHEN 'CASH' THEN N'Tiền Mặt' WHEN 'TRANSFER' THEN N'Chuyển Khoản' ... END AS [Phương Thức Thanh Toán]
              * invoice_status → CASE invoice_status WHEN 'PAID' THEN N'Đã Thanh Toán' WHEN 'PENDING' THEN N'Chờ Thanh Toán' ... END AS [Trạng Thái]
            - Xem danh sách đầy đủ mapping enum ở phần "MAPPING ENUM SANG TIẾNG VIỆT" bên dưới
            
            DANH SÁCH ALIAS TIẾNG VIỆT CÓ DẤU PHỔ BIẾN (tham khảo):
            - invoice_id → Mã Hóa Đơn
            - invoice_code → Mã Hóa Đơn Text
            - invoice_status → Trạng Thái (PHẢI dùng CASE WHEN convert)
            - invoice_channel → Kênh (PHẢI dùng CASE WHEN convert)
            - payment_method → Phương Thức Thanh Toán (PHẢI dùng CASE WHEN convert)
            - final_amount → Tổng Tiền
            - subtotal_amount → Tiền Trước Thuế
            - paid_at → Ngày Thanh Toán
            - reserved_at → Thời Gian Đặt Bàn
            - checked_in_at → Ngày Check In
            - reservation_code → Mã Đặt Bàn
            - reservation_note → Ghi Chú Đặt Bàn
            - guest_count → Số Khách
            - guest_name → Tên Khách
            - guest_phone → Số Điện Thoại Khách
            - product_name → Tên Món
            - product_id → Mã Món
            - quantity → Số Lượng
            - unit_price → Đơn Giá
            - table_number → Số Bàn
            - table_id → Mã Bàn
            - customer_name / full_name → Tên Khách Hàng
            - phone_number → Số Điện Thoại
            - email → Email
            - employee_id → Mã Nhân Viên
            - role → Vai Trò (PHẢI dùng CASE WHEN convert)
            - status → Trạng Thái (PHẢI dùng CASE WHEN convert)
            - capacity → Sức Chứa
            - unitPrice → Giá *Chú ý unitPrice*
            - combo_id → Mã Combo
            - combo_name → Tên Combo
            - SUM(final_amount) → Tổng Doanh Thu
            - COUNT(*) → Số Lượng
            - AVG(final_amount) → Trung Bình
            - CAST(paid_at AS DATE) → Ngày
            - MONTH(paid_at) → Tháng
            - YEAR(paid_at) → Năm
            - DATEPART(HOUR, paid_at) → Giờ
            - DATEPART(WEEKDAY, paid_at) → Thứ
            
            BUSINESS CONTEXT:
            - Invoice: Hóa đơn thanh toán (CŨNG CHỨA THÔNG TIN ĐẶT BÀN)
              * invoice_id: ID hóa đơn
              * invoice_code: Mã hóa đơn
              * invoice_status: Trạng thái (PENDING, PAID, CANCELLED)
              * invoice_channel: Kênh (ONLINE, OFFLINE)
              * payment_method: Phương thức thanh toán (CASH, TRANSFER, CARD, POINTS)
              * final_amount: Tổng tiền cuối cùng (dùng cho doanh thu)
              * subtotal_amount: Tổng tiền trước thuế/phí
              * paid_at: Thời điểm thanh toán (QUAN TRỌNG: dùng cột này cho doanh thu)
              * reserved_at: Thời điểm đặt bàn (NULL nếu không phải đặt bàn)
              * checked_in_at: Thời điểm check-in
              * reservation_code: Mã đặt bàn (NULL nếu không phải đặt bàn)
              * reservation_note: Ghi chú đặt bàn
              * guest_count: Số lượng khách
              * guest_name: Tên khách (nếu không có customer_id)
              * guest_phone: Số điện thoại khách
            - InvoiceItem: Chi tiết món ăn trong hóa đơn
              * invoice_id: ID hóa đơn
              * product_id: ID món ăn
              * combo_id: ID combo
              * quantity: Số lượng
              * unit_price: Giá đơn vị
            - Product: Món ăn/sản phẩm
              * product_id: ID món ăn
              * product_name: Tên món
              * price: Giá
              * status: Trạng thái (ACTIVE, INACTIVE)
            - ProductCombo: Combo món ăn
              * combo_id: ID combo
              * combo_name: Tên combo
              * price: Giá combo
            - DiningTable: Bàn ăn
              * table_id: ID bàn
              * table_number: Số bàn
              * capacity: Sức chứa
              * status: Trạng thái (AVAILABLE, OCCUPIED, RESERVED, CLEANING)
            - InvoiceDiningTable: Liên kết giữa hóa đơn và bàn
              * invoice_id: ID hóa đơn
              * table_id: ID bàn
            - Customer: Khách hàng
              * customer_id: ID khách hàng
              * full_name: Tên đầy đủ
              * phone_number: Số điện thoại
              * email: Email
            - Employee: Nhân viên
              * employee_id: ID nhân viên
              * full_name: Tên đầy đủ
              * role: Vai trò (ADMIN, RECEPTION, STAFF, CHEF)
            
            QUAN TRỌNG: Bạn PHẢI trả về ĐÚNG format JSON như sau, KHÔNG được thêm text nào khác:
            {
              "sql": "SELECT ... FROM ... WHERE ...",
              "explanation": "Câu query này sẽ lấy ra..."
            }
            
            CHỈ trả về JSON object, KHÔNG thêm markdown, KHÔNG thêm giải thích bên ngoài JSON.
            
            """ + databaseSchema;
        
        return prompt;
    }
    
    /**
     * Gọi OpenAI API
     */
    private String callOpenAI(String systemPrompt, String userQuestion) {
        try {
            // Tạo request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openaiModel);
            requestBody.put("temperature", 0.1); // Giảm creativity để SQL chính xác hơn
            requestBody.put("max_tokens", 1000);
            
            // Bắt buộc response format là JSON
            Map<String, String> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);
            
            // Messages
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userQuestion)
            );
            requestBody.put("messages", messages);
            
            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);
            
            // Call API
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                openaiApiUrl,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0)
                               .path("message").path("content").asText();
            
            return content;
            
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Lỗi khi gọi OpenAI API: " + e.getMessage());
        }
    }
    
    /**
     * Parse AI response thành AiQueryResponse
     */
    private AiQueryResponse parseAiResponse(String aiResponse, String originalQuestion) {
        try {
            // AI trả về JSON, parse nó
            // Loại bỏ markdown code block nếu có
            String cleanedResponse = aiResponse
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
            
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);
            
            String sql = jsonNode.path("sql").asText();
            String explanation = jsonNode.path("explanation").asText();
            
            // Nếu SQL rỗng, đây là rejection response (AI từ chối yêu cầu không hợp lệ)
            // Trả về response với SQL rỗng và explanation giải thích lý do từ chối
            if (sql == null || sql.trim().isEmpty()) {
                return AiQueryResponse.builder()
                    .generatedSql("")
                    .explanation(explanation)
                    .originalQuestion(originalQuestion)
                    .build();
            }
            
            // Validate SQL (phải là SELECT) - chỉ validate khi SQL không rỗng
            if (!sql.trim().toUpperCase().startsWith("SELECT")) {
                throw new SecurityException("Chỉ cho phép SELECT query");
            }
            
            return AiQueryResponse.builder()
                .generatedSql(sql)
                .explanation(explanation)
                .originalQuestion(originalQuestion)
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing AI response", e);
            throw new RuntimeException("Không thể parse response từ AI: " + e.getMessage());
        }
    }
}
