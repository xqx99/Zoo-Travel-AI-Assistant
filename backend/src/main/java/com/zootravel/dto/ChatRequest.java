package com.zootravel.dto;

/**
 * 聊天请求体
 */
public record ChatRequest(String sessionId, String message) {
}
