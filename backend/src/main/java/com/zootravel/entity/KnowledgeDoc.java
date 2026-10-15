package com.zootravel.entity;

/**
 * RAG 知识库源文档
 */
public record KnowledgeDoc(
        Long id,
        String title,
        String category,
        String content,
        String docSource) {
}
