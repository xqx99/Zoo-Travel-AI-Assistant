package com.zootravel.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话记忆存储于 Redis（key: zootravel:chat:{sessionId}）
 * 序列化为 [{"role":"user|ai|system","text":"..."}]
 */
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${zootravel.chat.history-key-prefix:zootravel:chat:}")
    private String keyPrefix;

    public RedisChatMemoryStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = redis.opsForValue().get(key(memoryId));
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode arr = mapper.readTree(json);
            List<ChatMessage> msgs = new ArrayList<>();
            for (JsonNode n : arr) {
                String role = n.get("role").asText();
                String text = n.get("text").asText();
                switch (role) {
                    case "user" -> msgs.add(UserMessage.from(text));
                    case "ai" -> msgs.add(AiMessage.from(text));
                    case "system" -> msgs.add(SystemMessage.from(text));
                    default -> { /* 忽略未知类型 */ }
                }
            }
            return msgs;
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        ArrayNode arr = mapper.createArrayNode();
        for (ChatMessage m : messages) {
            ObjectNode o = mapper.createObjectNode();
            if (m instanceof UserMessage u) {
                o.put("role", "user");
                o.put("text", u.singleText() == null ? "" : u.singleText());
            } else if (m instanceof AiMessage a) {
                o.put("role", "ai");
                o.put("text", a.text() == null ? "" : a.text());
            } else if (m instanceof SystemMessage s) {
                o.put("role", "system");
                o.put("text", s.text());
            } else {
                continue;
            }
            arr.add(o);
        }
        redis.opsForValue().set(key(memoryId), arr.toString());
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redis.delete(key(memoryId));
    }

    private String key(Object memoryId) {
        return keyPrefix + memoryId;
    }
}
