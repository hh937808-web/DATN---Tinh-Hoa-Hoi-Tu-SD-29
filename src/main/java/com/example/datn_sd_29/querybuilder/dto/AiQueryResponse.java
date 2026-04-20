package com.example.datn_sd_29.querybuilder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiQueryResponse {
    private String generatedSql;     // SQL được AI tạo ra
    private String explanation;      // Giải thích bằng tiếng Việt
    private String originalQuestion; // Câu hỏi gốc
}
