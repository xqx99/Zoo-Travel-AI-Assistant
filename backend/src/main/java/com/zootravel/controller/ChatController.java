package com.zootravel.controller;

import com.zootravel.dto.ApiResponse;
import com.zootravel.dto.ChatRequest;
import com.zootravel.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 聊天接口（SSE 流式）
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * SSE 流式对话：POST /api/chat/stream
     * body: {"sessionId":"s-001","message":"..."}
     * 事件 data: {"type":"meta|delta|done|error", ...}
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=utf-8")
    public SseEmitter stream(@RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(300_000L);
        chatService.stream(req.sessionId(), req.message(), emitter);
        return emitter;
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, String>>> history(@RequestParam String sessionId) {
        return ApiResponse.ok(chatService.history(sessionId));
    }

    @DeleteMapping("/history")
    public ApiResponse<Void> clearHistory(@RequestParam String sessionId) {
        chatService.clearHistory(sessionId);
        return ApiResponse.ok();
    }
}
