package com.zootravel.service;

import com.zootravel.entity.KnowledgeDoc;
import com.zootravel.repository.KnowledgeRepository;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动时初始化 Redis 向量库（幂等：索引已存在且有文档则跳过）。
 * 知识文档从 MySQL knowledge_doc 读取 → 切分 → 本地嵌入 → 写入 Redis(RediSearch)。
 */
@Component
public class VectorIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VectorIndexInitializer.class);

    private final KnowledgeRepository knowledgeRepository;
    private final EmbeddingModel embeddingModel;
    private final DocumentSplitter splitter;
    private final JedisPooled jedis;
    private final String indexName;
    private final int dimension;

    public VectorIndexInitializer(KnowledgeRepository knowledgeRepository,
                                  EmbeddingModel embeddingModel,
                                  DocumentSplitter splitter,
                                  JedisPooled jedis,
                                  @Value("${zootravel.rag.vector-index:zoo_kb}") String indexName,
                                  @Value("${zootravel.rag.dimension:384}") int dimension) {
        this.knowledgeRepository = knowledgeRepository;
        this.embeddingModel = embeddingModel;
        this.splitter = splitter;
        this.jedis = jedis;
        this.indexName = indexName;
        this.dimension = dimension;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (indexHasDocs()) {
                log.info("RAG 向量索引 {} 已存在且含文档，跳过重建", indexName);
                return;
            }
            rebuild();
        } catch (Exception e) {
            log.warn("RAG 向量库初始化失败（请确认 Redis/RediSearch 已启动）：{}", e.getMessage());
        }
    }

    /** 强制重建（/api/knowledge/reindex 调用） */
    public synchronized void rebuild() {
        log.info("开始重建 RAG 向量索引 {}", indexName);
        dropIndexIfExists();
        deletePrefixKeys();
        // 重建索引需要新建 store 实例（其构造会执行 FT.CREATE）
        EmbeddingStore<TextSegment> store = RedisEmbeddingStore.builder()
                .unifiedJedis(jedis)
                .indexName(indexName)
                .prefix(indexName + ":")
                .dimension(dimension)
                .build();
        List<KnowledgeDoc> docs = knowledgeRepository.findAll();
        int total = 0;
        for (KnowledgeDoc doc : docs) {
            Map<String, String> meta = new HashMap<>();
            meta.put("title", doc.title() == null ? "" : doc.title());
            meta.put("category", doc.category() == null ? "" : doc.category());
            Document document = Document.from(doc.content(), Metadata.from(meta));
            List<TextSegment> segments = splitter.split(document);
            if (segments.isEmpty()) {
                continue;
            }
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            store.addAll(embeddings, segments);
            total += segments.size();
        }
        log.info("RAG 向量索引 {} 初始化完成：共 {} 个文档分片", indexName, total);
    }

    private boolean indexHasDocs() {
        try {
            Object numDocs = jedis.ftInfo(indexName).get("num_docs");
            return numDocs != null && ((Number) numDocs).longValue() > 0;
        } catch (JedisDataException e) {
            return false; // 索引不存在
        }
    }

    private void dropIndexIfExists() {
        try {
            jedis.ftDropIndex(indexName);
            log.info("已删除旧向量索引 {}", indexName);
        } catch (JedisDataException e) {
            log.info("旧向量索引 {} 不存在，无需删除", indexName);
        }
    }

    private void deletePrefixKeys() {
        String pattern = indexName + ":*";
        List<String> keys = new ArrayList<>(jedis.keys(pattern));
        if (!keys.isEmpty()) {
            jedis.del(keys.toArray(new String[0]));
            log.info("清理旧向量数据 {} 条", keys.size());
        }
    }
}
