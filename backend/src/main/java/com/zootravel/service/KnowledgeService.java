package com.zootravel.service;

import com.zootravel.entity.KnowledgeDoc;
import com.zootravel.repository.KnowledgeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 知识文档业务
 */
@Service
public class KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;

    public KnowledgeService(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    public List<KnowledgeDoc> list() {
        return knowledgeRepository.findAll();
    }

    public Optional<KnowledgeDoc> get(Long id) {
        return knowledgeRepository.findById(id);
    }

    public long save(KnowledgeDoc doc) {
        return knowledgeRepository.save(doc);
    }

    public boolean update(KnowledgeDoc doc) {
        return knowledgeRepository.update(doc) > 0;
    }

    public boolean delete(Long id) {
        return knowledgeRepository.delete(id) > 0;
    }
}
