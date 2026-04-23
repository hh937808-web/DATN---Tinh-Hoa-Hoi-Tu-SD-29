package com.example.datn_sd_29.chatbot.service;

import com.example.datn_sd_29.blog.entity.BlogPost;
import com.example.datn_sd_29.blog.repository.BlogPostRepository;
import com.example.datn_sd_29.product.entity.Product;
import com.example.datn_sd_29.product.enums.ProductStatus;
import com.example.datn_sd_29.product.repository.ProductRepository;
import com.example.datn_sd_29.product_combo.entity.ProductCombo;
import com.example.datn_sd_29.product_combo.repository.ProductComboRepository;
import com.example.datn_sd_29.voucher.entity.CustomerVoucher;
import com.example.datn_sd_29.voucher.entity.ProductVoucher;
import com.example.datn_sd_29.voucher.repository.CustomerVoucherRepository;
import com.example.datn_sd_29.voucher.repository.ProductVoucherRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBotService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.base-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    private final ProductRepository productRepository;
    private final ProductComboRepository productComboRepository;
    private final ProductVoucherRepository productVoucherRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final BlogPostRepository blogPostRepository;

    public void streamAnswer(List<Map<String, String>> history, OutputStream outputStream) throws IOException {
        String systemPrompt = buildSystemPrompt();

        // Build messages array: system + history (giới hạn 20 tin gần nhất)
        StringBuilder messagesJson = new StringBuilder();
        messagesJson.append("[");
        messagesJson.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(systemPrompt)).append("\"}");

        int start = Math.max(0, history.size() - 20);
        for (int i = start; i < history.size(); i++) {
            Map<String, String> msg = history.get(i);
            String role = "user".equals(msg.get("role")) ? "user" : "assistant";
            messagesJson.append(",{\"role\":\"").append(role)
                         .append("\",\"content\":\"").append(escapeJson(msg.getOrDefault("content", ""))).append("\"}");
        }
        messagesJson.append("]");

        String jsonBody = """
            {
              "model": "%s",
              "stream": true,
              "temperature": 0.3,
              "max_tokens": 1200,
              "messages": %s
            }
            """.formatted(model, messagesJson.toString());

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;
                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) break;

                String content = extractDeltaContent(data);
                if (content != null && !content.isEmpty()) {
                    writer.write("data:" + content.replace("\n", "\\n") + "\n\n");
                    writer.flush();
                }
            }
        }

        writer.write("data:[DONE]\n\n");
        writer.flush();
        conn.disconnect();
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        String now = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String todayDate = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // === NHÂN CÁCH ===
        sb.append("Bạn là ByHat Assistant - trợ lý ảo thân thiện của quán lẩu ByHat.\n");
        sb.append("Thời gian hiện tại: ").append(now).append("\n");
        sb.append("Ngày hôm nay (format API): ").append(todayDate).append("\n\n");

        sb.append("PHONG CÁCH GIAO TIẾP:\n");
        sb.append("- Xưng 'mình', gọi khách là 'bạn'\n");
        sb.append("- Thân thiện, nhiệt tình, như bạn bè giới thiệu quán cho nhau\n");
        sb.append("- Dùng emoji phù hợp nhưng không spam\n");
        sb.append("- Trả lời ngắn gọn, dễ hiểu, chia dòng rõ ràng\n");
        sb.append("- Chủ động gợi ý thêm: 'Bạn muốn mình gợi ý thêm không?', 'Bạn muốn đặt bàn luôn không?'\n\n");

        // === THÔNG TIN QUÁN ===
        sb.append("THÔNG TIN QUÁN:\n");
        sb.append("- Tên: ByHat Hotpot Restaurant\n");
        sb.append("- Địa chỉ: Số 1 Trịnh Văn Bô, Quận Nam Từ Liêm\n");
        sb.append("- SĐT: 0123456789\n");
        sb.append("- Email: anbach4865@gmail.com\n");
        sb.append("- Giờ mở cửa: 9h sáng - 11h30 tối, cả tuần không nghỉ\n");
        sb.append("- Đặt bàn online trên website, tích điểm thành viên, voucher giảm giá\n\n");

        // === SẢN PHẨM ===
        List<Product> products = productRepository.findByAvailabilityStatusNot(ProductStatus.DISCONTINUED);
        sb.append("DANH SÁCH MÓN ĂN ĐANG BÁN (").append(products.size()).append(" món):\n");
        for (Product p : products) {
            sb.append("- ").append(p.getProductName());
            sb.append(" | Giá: ").append(formatPrice(p.getUnitPrice()));
            sb.append(" | Loại: ").append(getCategoryName(p.getProductCategory() != null ? p.getProductCategory().name() : ""));
            if (p.getDescription() != null && !p.getDescription().isBlank()) sb.append(" | ").append(p.getDescription());
            sb.append("\n");
        }

        // === COMBO ===
        List<ProductCombo> combos = productComboRepository.findByIsActiveTrue();
        sb.append("\nDANH SÁCH COMBO (").append(combos.size()).append(" combo):\n");
        for (ProductCombo c : combos) {
            sb.append("- ").append(c.getComboName());
            sb.append(" | Giá: ").append(formatPrice(c.getComboPrice()));
            if (c.getDescription() != null && !c.getDescription().isBlank()) sb.append(" | ").append(c.getDescription());
            sb.append("\n");
        }

        // === VOUCHER GIẢM GIÁ MÓN ===
        List<ProductVoucher> productVouchers = productVoucherRepository.findByIsActiveTrue();
        List<ProductVoucher> activeProductVouchers = productVouchers.stream()
                .filter(v -> v.getRemainingQuantity() != null && v.getRemainingQuantity() > 0)
                .toList();
        if (!activeProductVouchers.isEmpty()) {
            sb.append("\nƯU ĐÃI GIẢM GIÁ MÓN ĂN:\n");
            for (ProductVoucher v : activeProductVouchers) {
                sb.append("- Giảm ").append(v.getDiscountPercent()).append("%");
                sb.append(" cho món ").append(v.getProduct().getProductName());
                if (v.getValidTo() != null) sb.append(" (đến ").append(v.getValidTo()).append(")");
                sb.append("\n");
            }
        }

        // === VOUCHER GIẢM GIÁ HÓA ĐƠN ===
        List<CustomerVoucher> invoiceVouchers = customerVoucherRepository.findPublicVouchers();
        List<CustomerVoucher> activeInvoiceVouchers = invoiceVouchers.stream()
                .filter(v -> v.getVoucherStatus() != null
                        && "HOAT_DONG".equals(v.getVoucherStatus().name())
                        && v.getRemainingQuantity() != null && v.getRemainingQuantity() > 0)
                .toList();
        if (!activeInvoiceVouchers.isEmpty()) {
            sb.append("\nƯU ĐÃI GIẢM GIÁ HÓA ĐƠN:\n");
            for (CustomerVoucher v : activeInvoiceVouchers) {
                sb.append("- ").append(v.getPersonalVoucher().getVoucherName());
                sb.append(" | Giảm ").append(v.getPersonalVoucher().getDiscountPercent()).append("% trên tổng hóa đơn");
                if (v.getPersonalVoucher().getMinOrderAmount() != null) {
                    sb.append(" | Hóa đơn từ ").append(formatPrice(v.getPersonalVoucher().getMinOrderAmount()));
                }
                if (v.getExpiresAt() != null) sb.append(" | HSD: ").append(v.getExpiresAt());
                sb.append("\n");
            }
        }

        // === BÀI VIẾT / TIN TỨC ===
        List<BlogPost> posts = blogPostRepository.findActivePublished(java.time.Instant.now());
        if (!posts.isEmpty()) {
            sb.append("\nBÀI VIẾT & TIN TỨC (tổng cộng ").append(posts.size()).append(" bài, khi khách hỏi phải liệt kê TẤT CẢ):\n");
            for (int idx = 0; idx < posts.size(); idx++) {
                BlogPost post = posts.get(idx);
                sb.append(idx + 1).append(". ID=").append(post.getId());
                sb.append(" | Danh mục: ").append(post.getCategory() != null ? post.getCategory() : "Tin tức");
                sb.append(" | Tiêu đề: ").append(post.getTitle());
                if (post.getPublishedAt() != null) {
                    sb.append(" | Ngày: ").append(
                        java.time.LocalDate.ofInstant(post.getPublishedAt(), java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    );
                }
                sb.append("\n");

                // Nội dung chính — giới hạn 300 ký tự
                String content = "";
                if (post.getContent() != null && !post.getContent().isBlank()) {
                    content = post.getContent().replace("\n", " ").trim();
                } else if (post.getSummary() != null && !post.getSummary().isBlank()) {
                    content = post.getSummary();
                }
                if (!content.isEmpty()) {
                    if (content.length() > 300) content = content.substring(0, 300) + "...";
                    sb.append("   Nội dung: ").append(content).append("\n");
                }
                sb.append("   Link: /posts/").append(post.getId()).append("\n");
            }
        }

        // === QUY TẮC ===
        sb.append("\nQUY TẮC TRẢ LỜI:\n");
        sb.append("- Chỉ trả lời dựa trên thông tin ở trên, KHÔNG trả lời câu hỏi ngoòa luồng\n");
        sb.append("- Nếu không biết → 'Mình chưa có thông tin này, bạn liên hệ quán qua SĐT 0123456789 nhé!'\n");
        sb.append("- Khi gợi ý món → kèm giá\n");
        sb.append("- Khi nói về ưu đãi → nói rõ giảm bao nhiêu %, cho món gì hoặc hóa đơn từ bao nhiêu\n");
        sb.append("- Khi có sự kiện/bài viết liên quan → chủ động giới thiệu nội dung chính cho khách\n");
        sb.append("- Khi nói về bài viết/tin tức → tóm tắt nội dung chính VÀ kèm link xem chi tiết theo format: [Xem chi tiết](/posts/ID)\n");
        sb.append("  Ví dụ: 'Bạn có thể xem chi tiết tại đây: [Xem chi tiết](/posts/5)'\n");
        sb.append("- KHÔNG hiển thị mã sản phẩm, mã combo cho khách\n");
        sb.append("- KHÔNG nói số lượt còn lại của voucher\n");
        sb.append("\nPHẠM VI HỖ TRỢ:\n");
        sb.append("- Bạn CHỈ hỗ trợ các chủ đề liên quan đến nhà hàng ByHat: thực đơn, giá cả, đặt bàn, ưu đãi, tin tức quán, thông tin quán.\n");
        sb.append("- Với lời chào hỏi thân thiện (xin chào, hello, hi, chào bạn...) → vui vẻ chào lại và giới thiệu ngắn gọn mình có thể giúp gì.\n");
        sb.append("- Với câu hỏi NGOÀI phạm vi nhà hàng (code, lập trình, toán học, lịch sử, khoa học, dịch thuật, viết bài, tư vấn cá nhân...) → từ chối nhẹ nhàng, thân thiện, VÍ DỤ:\n");
        sb.append("  'Xin lỗi mình chỉ trợ lý của quán lẩu ByHat thôi nè 😄 Mình có thể giúp bạn xem thực đơn, đặt bàn hoặc tìm ưu đãi nhé!'\n");
        sb.append("- TUYỆT ĐỐI không trả lời nội dung ngoài luồng dù khách yêu cầu nhiều lần.\n");

        // === ĐẶT BÀN ===
        sb.append("\nĐẶT BÀN:\n");
        sb.append("- Khi khách muốn đặt bàn, hỏi lần lượt: ngày, giờ (9h-23h30), số khách\n");
        sb.append("- Hỏi: 'Bạn đặt cho mình hay đặt hộ người khác?'\n");
        sb.append("  + Đặt cho mình: guestName và guestPhone để trống\n");
        sb.append("  + Đặt hộ: hỏi tên + SĐT người dùng bữa\n");
        sb.append("- Khi đủ info → trả JSON block:\n");
        sb.append("```booking\n");
        sb.append("{\"date\":\"YYYY-MM-DD\",\"time\":\"HH:mm\",\"guestCount\":X,\"note\":\"\",\"guestName\":\"\",\"guestPhone\":\"\"}\n");
        sb.append("```\n");
        sb.append("- Kèm: 'Mình đã chuẩn bị thông tin đặt bàn, bạn kiểm tra và xác nhận bên dưới nhé! 😊'\n");
        sb.append("- Ngày từ ").append(todayDate).append(" trở đi, KHÔNG ngày quá khứ\n");
        sb.append("- Hôm nay thì giờ phải SAU ").append(now).append("\n");
        sb.append("- 'hôm nay'/'ngày mai'/'cuối tuần' → tính ra YYYY-MM-DD chính xác\n");

        return sb.toString();
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "0đ";
        return String.format("%,.0f", price) + "đ";
    }

    private String getCategoryName(String cat) {
        return switch (cat) {
            case "RAW_FOOD" -> "Thực phẩm sống";
            case "COOKED_FOOD" -> "Món ăn chín";
            case "HOT_POT_BROTH" -> "Nước lẩu";
            case "DRINK" -> "Đồ uống";
            case "DESSERT" -> "Tráng miệng";
            default -> cat;
        };
    }

    private String extractDeltaContent(String json) {
        int deltaIdx = json.indexOf("\"delta\"");
        if (deltaIdx == -1) return null;
        int contentIdx = json.indexOf("\"content\"", deltaIdx);
        if (contentIdx == -1) return null;
        int colonIdx = json.indexOf(":", contentIdx);
        if (colonIdx == -1) return null;
        int startQuote = json.indexOf("\"", colonIdx + 1);
        if (startQuote == -1) return null;

        StringBuilder result = new StringBuilder();
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') break;
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n' -> { result.append('\n'); i++; }
                    case 't' -> { result.append('\t'); i++; }
                    case '"' -> { result.append('"'); i++; }
                    case '\\' -> { result.append('\\'); i++; }
                    default -> result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
