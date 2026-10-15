package com.zootravel.controller;

import com.zootravel.dto.ApiResponse;
import com.zootravel.entity.KnowledgeDoc;
import com.zootravel.service.KnowledgeService;
import com.zootravel.service.VectorIndexInitializer;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库文档管理 + 向量索引重建
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final VectorIndexInitializer vectorIndexInitializer;

    public KnowledgeController(KnowledgeService knowledgeService,
                               VectorIndexInitializer vectorIndexInitializer) {
        this.knowledgeService = knowledgeService;
        this.vectorIndexInitializer = vectorIndexInitializer;
    }

    @GetMapping("/list")
    public ApiResponse<List<KnowledgeDoc>> list() {
        return ApiResponse.ok(knowledgeService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDoc> get(@PathVariable Long id) {
        return knowledgeService.get(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("文档不存在: " + id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody KnowledgeDoc doc) {
        return ApiResponse.ok(knowledgeService.save(doc));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody KnowledgeDoc doc) {
        if (knowledgeService.update(doc)) {
            return ApiResponse.ok();
        }
        return ApiResponse.fail("文档不存在: " + id);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        if (knowledgeService.delete(id)) {
            return ApiResponse.ok();
        }
        return ApiResponse.fail("文档不存在: " + id);
    }

    /** 强制重建向量索引 */
    @PostMapping("/reindex")
    public ApiResponse<Void> reindex() {
        vectorIndexInitializer.rebuild();
        return ApiResponse.ok();
    }
}
