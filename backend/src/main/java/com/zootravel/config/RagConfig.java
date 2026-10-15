package com.zootravel.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * RAG 装配：本地 ONNX 嵌入模型 + Redis(RediSearch) 向量库 + 检索器
 * 显式声明 Bean，规避 starter 自动装配的版本不确定性
 */
@Configuration
public class RagConfig {

    @Value("${zootravel.rag.vector-index:zoo_kb}")
    private String indexName;

    @Value("${zootravel.rag.chunk-size:500}")
    private int chunkSize;

    @Value("${zootravel.rag.overlap:100}")
    private int overlap;

    @Value("${zootravel.rag.max-results:4}")
    private int maxResults;

    @Value("${zootravel.rag.min-score:0.6}")
    private double minScore;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(JedisPooled jedisPooled) {
        return RedisEmbeddingStore.builder()
                .unifiedJedis(jedisPooled)
                .indexName(indexName)
                .prefix(indexName + ":")
                .dimension(384)
                .build();
    }

    @Bean
    public DocumentSplitter documentSplitter() {
        return DocumentSplitters.recursive(chunkSize, overlap);
    }

    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore,
                                             EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }
}
