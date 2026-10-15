package com.zootravel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zootravel.dto.Quote;
import com.zootravel.memory.RedisChatMemoryStore;
import com.zootravel.service.IntentService.Intent;
import com.zootravel.service.ZooService.StructuredResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 聊天编排：意图识别 → 双路检索（MySQL 结构化 + RAG 攻略）→ SSE 流式生成
 * DeepSeek key 缺失时降级为数据库直拼模板回答
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ZooService zooService;
    private final IntentService intentService;
    private final ContentRetriever contentRetriever;
    private final StreamingChatModel streamingChatModel;
    private final RedisChatMemoryStore memoryStore;
    private final ObjectMapper objectMapper;
    private final String systemPrompt;
    private final boolean localFallback;
    private final String apiKey;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatService(ZooService zooService,
                       IntentService intentService,
                       ContentRetriever contentRetriever,
                       StreamingChatModel streamingChatModel,
                       RedisChatMemoryStore memoryStore,
                       ObjectMapper objectMapper,
                       @Value("classpath:prompt/system.md") Resource systemPromptResource,
                       @Value("${zootravel.chat.local-fallback:true}") boolean localFallback,
                       @Value("${langchain4j.open-ai.streaming-chat-model.api-key:}") String apiKey) throws IOException {
        this.zooService = zooService;
        this.intentService = intentService;
        this.contentRetriever = contentRetriever;
        this.streamingChatModel = streamingChatModel;
        this.memoryStore = memoryStore;
        this.objectMapper = objectMapper;
        this.systemPrompt = new String(systemPromptResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        this.localFallback = localFallback;
        this.apiKey = apiKey;
    }

    /**
     * 异步执行聊天，结果以 SSE 事件写入 emitter
     */
    public void stream(String sessionId, String message, SseEmitter emitter) {
        executor.submit(() -> {
            try {
                String sid = (sessionId == null || sessionId.isBlank())
                        ? UUID.randomUUID().toString()
                        : sessionId;

                // 1. 意图识别
                Intent intent = intentService.analyze(message);

                // 2. 双路检索
                StructuredResult structured = zooService.chatSearch(intent.destination(), intent.animal());
                List<Quote> quotes = new ArrayList<>(structured.quotes());
                List<Content> ragContents = contentRetriever.retrieve(Query.from(message));
                StringBuilder ragText = new StringBuilder();
                for (Content c : ragContents) {
                    String title = c.textSegment().metadata().getString("title");
                    if (title == null || title.isBlank()) title = "知识库";
                    quotes.add(new Quote("rag", null, title, truncate(c.textSegment().text(), 120)));
                    ragText.append("【").append(title).append("】").append(c.textSegment().text()).append("\n");
                }

                // 3. 下发检索引用
                Map<String, Object> meta = new HashMap<>();
                meta.put("type", "meta");
                meta.put("sessionId", sid);
                meta.put("quotes", quotes);
                sendEvent(emitter, meta);

                // 4. 会话记忆（含本条用户消息）
                MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                        .id(sid)
                        .maxMessages(20)
                        .chatMemoryStore(memoryStore)
                        .build();
                memory.add(UserMessage.from(message));

                // 5. 组装 prompt：系统提示 + 参考信息 + 历史对话
                List<ChatMessage> msgs = new ArrayList<>();
                msgs.add(SystemMessage.from(systemPrompt));
                if (!structured.text().isBlank() || !ragText.isEmpty()) {
                    String context = "【检索到的参考信息】\n"
                            + (structured.text().isBlank() ? "" : "【结构化数据】\n" + structured.text() + "\n")
                            + (ragText.isEmpty() ? "" : "【知识库攻略】\n" + ragText);
                    msgs.add(UserMessage.from(context));
                }
                msgs.addAll(memory.messages());

                // 6. 流式生成（有 key 走 LLM，否则本地降级）
                if (!apiKey.isBlank()) {
                    streamWithLlm(emitter, memory, msgs);
                } else {
                    streamWithFallback(emitter, memory, intent, structured, ragText.toString(), message);
                }
            } catch (Exception e) {
                log.error("聊天流式失败", e);
                sendEvent(emitter, Map.of("type", "error", "message", e.getMessage()));
                emitter.complete();
            }
        });
    }

    private void streamWithLlm(SseEmitter emitter, MessageWindowChatMemory memory, List<ChatMessage> msgs) {
        streamingChatModel.chat(ChatRequest.builder().messages(msgs).build(),
                new StreamingChatResponseHandler() {
                    private final StringBuilder sb = new StringBuilder();

                    @Override
                    public void onPartialResponse(String token) {
                        sb.append(token);
                        sendEvent(emitter, Map.of("type", "delta", "content", token));
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        memory.add(AiMessage.from(sb.toString()));
                        Map<String, Object> usage = new HashMap<>();
                        TokenUsage tu = response.tokenUsage();
                        if (tu != null) {
                            usage.put("inputTokens", tu.inputTokenCount());
                            usage.put("outputTokens", tu.outputTokenCount());
                        }
                        sendEvent(emitter, Map.of("type", "done", "usage", usage));
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("LLM 流式调用失败", error);
                        sendEvent(emitter, Map.of("type", "error", "message", error.getMessage()));
                        emitter.complete();
                    }
                });
    }

    private void streamWithFallback(SseEmitter emitter, MessageWindowChatMemory memory,
                                    Intent intent, StructuredResult structured,
                                    String ragText, String message) {
        StringBuilder answer = new StringBuilder();
        answer.append("> 当前未配置 DeepSeek API Key，以下为基于数据库信息为你生成的简要规划（配置 DEEPSEEK_API_KEY 环境变量后即可获得 AI 深度规划）。\n\n");
        if (intent.destination() != null) {
            answer.append("## 目的地\n").append(intent.destination()).append("\n\n");
        }
        if (!structured.text().isBlank()) {
            answer.append("## 结构化数据\n").append(structured.text()).append("\n");
        }
        if (!ragText.isBlank()) {
            answer.append("## 攻略参考\n").append(ragText).append("\n");
        }
        if (intent.destination() == null && intent.animal() == null) {
            answer.append("你可以这样问我：\n")
                    .append("- \"周末带小孩去南京红山动物园看熊猫\"\n")
                    .append("- \"北京动物园门票多少钱，几点开放\"\n")
                    .append("- \"想去看大熊猫，推荐哪个动物园\"\n");
        }
        String text = answer.toString();
        memory.add(AiMessage.from(text));
        // 分段推送模拟流式
        for (int i = 0; i < text.length(); i += 30) {
            int end = Math.min(i + 30, text.length());
            sendEvent(emitter, Map.of("type", "delta", "content", text.substring(i, end)));
        }
        sendEvent(emitter, Map.of("type", "done", "usage", Map.of()));
        emitter.complete();
    }

    public List<Map<String, String>> history(String sessionId) {
        List<Map<String, String>> list = new ArrayList<>();
        for (ChatMessage m : memoryStore.getMessages(sessionId)) {
            Map<String, String> item = new HashMap<>();
            item.put("role", m.type().name().toLowerCase());
            String text = switch (m) {
                case UserMessage u -> u.singleText() == null ? "" : u.singleText();
                case AiMessage a -> a.text() == null ? "" : a.text();
                case SystemMessage s -> s.text();
                default -> "";
            };
            item.put("text", text);
            list.add(item);
        }
        return list;
    }

    public void clearHistory(String sessionId) {
        memoryStore.deleteMessages(sessionId);
    }

    private void sendEvent(SseEmitter emitter, Map<String, Object> event) {
        try {
            emitter.send(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.warn("SSE 发送失败", e);
            emitter.completeWithError(e);
        }
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
