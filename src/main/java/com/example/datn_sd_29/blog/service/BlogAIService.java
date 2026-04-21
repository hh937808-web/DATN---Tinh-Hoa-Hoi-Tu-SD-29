package com.example.datn_sd_29.blog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class BlogAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    /**
     * Stream bài viết từ OpenAI, ghi trực tiếp từng chunk SSE vào OutputStream của response.
     */
    public void streamArticle(String title, String summary, String category, OutputStream outputStream) throws IOException {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(title, summary, category);

        String sysEscaped = escapeJson(systemPrompt);
        String userEscaped = escapeJson(userPrompt);

        String jsonBody = """
            {
              "model": "%s",
              "stream": true,
              "temperature": 0.75,
              "max_tokens": 1500,
              "messages": [
                {"role": "system", "content": "%s"},
                {"role": "user", "content": "%s"}
              ]
            }
            """.formatted(model, sysEscaped, userEscaped);

        HttpURLConnection conn = (HttpURLConnection) new URL(OPENAI_URL).openConnection();
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
                    // Ghi SSE event thẳng ra response: data:nội dung\n\n
                    writer.write("data:" + content.replace("\n", "\\n") + "\n\n");
                    writer.flush();
                }
            }
        }

        // Signal done
        writer.write("data:[DONE]\n\n");
        writer.flush();
        conn.disconnect();

        log.info("AI stream hoàn tất cho tiêu đề: {}", title);
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

        StringBuilder sb = new StringBuilder();
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') break;
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String buildSystemPrompt() {
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        return """
                Bạn là copywriter marketing F&B chuyên viết bài cho quán lẩu ByHat (ByHat Hotpot Restaurant).
                Ngày hôm nay: %s

                Về ByHat:
                - Quán lẩu đa dạng: nhiều vị nước lẩu, thực phẩm sống (thịt, hải sản, rau), món ăn chín, đồ uống, tráng miệng
                - Giờ mở cửa: 9h sáng đến 11h30 tối
                - Phù hợp nhóm bạn, gia đình, đồng nghiệp, hẹn hò
                - Có đặt bàn online, tích điểm thành viên, voucher giảm giá
                - Giá hợp lý, không gian thoải mái

                Thông tin liên hệ quán (LUÔN ghi ở cuối bài):
                - SĐT: 0123456789
                - Email: anbach4865@gmail.com
                - Địa chỉ: Số 1 Trịnh Văn Bô, Quận Nam Từ Liêm
                - Đặt bàn online: trên website ByHat
                - Giờ mở cửa: 9h sáng - 11h30 tối

                Phong cách viết - BẮT BUỘC TUÂN THEO:
                - Viết kiểu marketing cuốn hút, có cảm xúc, tạo được sự hứng thú
                - Dùng dấu ** để in đậm** những từ khóa quan trọng, tên chương trình, con số nổi bật
                - Mở bài phải có câu hook mạnh, tạo tò mò hoặc cảm xúc ngay dòng đầu
                - Dùng emoji ở đầu đoạn hoặc đầu bullet point, tạo điểm nhấn thị giác
                - Chia đoạn rõ ràng, mỗi đoạn cách nhau 1 dòng trống
                - Khi liệt kê ưu đãi, dùng bullet points có emoji đầu dòng
                - Khoảng 300 - 500 từ
                - Giọng văn vừa chuyên nghiệp vừa thân thiện, như đang giới thiệu cho bạn bè
                - Cuối bài LUÔN có phần footer thông tin liên hệ theo đúng format sau:

                **Cuối bài BẮT BUỘC có block này (giữ nguyên format):**
                ---
                Thời gian áp dụng: [ghi rõ thời gian dựa theo nội dung bài]

                Cách thức đặt bàn/đặt món:
                📞 Gọi điện trực tiếp: **0123456789**
                📧 Email: **anbach4865@gmail.com**
                🌐 Đặt bàn online: trên website ByHat
                📍 Địa chỉ: **Số 1 Trịnh Văn Bô, Quận Nam Từ Liêm**
                🕐 Giờ mở cửa: **9h sáng - 11h30 tối**

                Hẹn gặp các bạn tại ByHat! 🍲
                ---

                - KHÔNG lặp tiêu đề, KHÔNG thêm hashtag
                - Chỉ trả nội dung bài viết

                Ví dụ phong cách mẫu:
                "🍲 Món ngon chất lượng, giá cực sốc chỉ có tại ByHat! Đừng bỏ lỡ cơ hội thưởng thức những món ăn tốt nhất với giá ưu đãi đặc biệt...

                🌿 Hương vị đa dạng, giá cả bất ngờ!

                Tại ByHat, chúng tôi luôn cố gắng mang đến những món ăn được chế biến từ nguyên liệu tươi ngon...

                Ưu đãi:
                🔥 **Giảm giá 20%%** toàn bộ menu (áp dụng cho cả ăn tại chỗ và mang về)
                🍧 **Tặng kem** món tráng miệng hấp dẫn cho mỗi hóa đơn từ 500.000đ
                🥤 **Free nước ngọt/trà đá** cho tất cả khách hàng

                Đừng chần chừ nữa, hãy **đặt bàn ngay hôm nay**! Số lượng có hạn, hãy nhanh tay...

                Thời gian áp dụng: Duy nhất trong tháng...
                📞 Gọi điện: **0123456789**
                📧 Email: **anbach4865@gmail.com**
                📍 Địa chỉ: **Số 1 Trịnh Văn Bô, Quận Nam Từ Liêm**
                🕐 Giờ mở cửa: **9h sáng - 11h30 tối**

                Hẹn gặp các bạn tại ByHat! 🍲"
                """.formatted(today);
    }

    private String buildUserPrompt(String title, String summary, String category) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Viết bài marketing cho quán lẩu ByHat.\n\n");
        prompt.append("Tiêu đề: ").append(title).append("\n");

        if (category != null && !category.isBlank()) {
            prompt.append("Danh mục: ").append(category).append("\n\n");

            switch (category) {
                case "Khuyến mãi" -> prompt.append(
                    "Yêu cầu: Viết content quảng bá chương trình ưu đãi.\n" +
                    "- Mở bài bằng hook gây tò mò hoặc FOMO (sợ bỏ lỡ)\n" +
                    "- Liệt kê ưu đãi cụ thể bằng bullet points\n" +
                    "- Ghi rõ thời gian, điều kiện áp dụng\n" +
                    "- CTA cuối bài: đặt bàn ngay / inbox để giữ chỗ\n");
                case "Ẩm thực" -> prompt.append(
                    "Yêu cầu: Viết content giới thiệu món ăn / nước lẩu.\n" +
                    "- Mở bài bằng tình huống thực tế (trời lạnh, đi nhóm bạn, cuối tuần...)\n" +
                    "- Mô tả món: vị gì, ăn kèm gì, phù hợp ai\n" +
                    "- Viết thực tế, gợi cảm giác muốn ăn thử\n" +
                    "- CTA: ghé quán thử / đặt bàn online\n");
                case "Sự kiện" -> prompt.append(
                    "Yêu cầu: Viết content thông báo sự kiện.\n" +
                    "- Mở bài tạo hứng thú\n" +
                    "- Nêu rõ: thời gian, nội dung sự kiện, quyền lợi khách\n" +
                    "- Tạo cảm giác đáng để đến\n" +
                    "- CTA: đăng ký / đặt chỗ sớm\n");
                case "Tin tức" -> prompt.append(
                    "Yêu cầu: Viết content thông báo tin mới về quán.\n" +
                    "- Ngắn gọn, rõ ràng, đi thẳng vào vấn đề\n" +
                    "- Nêu cái mới là gì, khách được lợi gì\n" +
                    "- CTA nhẹ nhàng cuối bài\n");
            }
        }

        if (summary != null && !summary.isBlank()) {
            prompt.append("\nÝ chính admin muốn truyền tải: ").append(summary).append("\n");
        }

        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        prompt.append("\nNgày đăng bài: ").append(today);
        prompt.append("\nLưu ý: dùng **in đậm** cho từ khóa, xuống dòng rõ ràng, cách đoạn bằng dòng trống. Viết bài ngay.");
        return prompt.toString();
    }
}
