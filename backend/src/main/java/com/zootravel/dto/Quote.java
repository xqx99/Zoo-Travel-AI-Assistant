package com.zootravel.dto;

/**
 * 检索引用（SSE meta 事件中下发，前端渲染引用卡片）
 */
public record Quote(String kind, Long zooId, String title, String text) {
}
