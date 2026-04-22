package com.example.datn_sd_29.chatbot.controller;

import com.example.datn_sd_29.chatbot.service.ChatBotService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    @PostMapping("/ask")
    public void ask(@RequestBody Map<String, Object> body, HttpServletResponse response) throws Exception {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) body.get("messages");
        if (history == null || history.isEmpty()) {
            response.setStatus(400);
            response.getWriter().write("Messages required");
            return;
        }

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        chatBotService.streamAnswer(history, response.getOutputStream());
        response.flushBuffer();
    }
}
