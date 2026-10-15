package com.zootravel.repository;

import com.zootravel.entity.KnowledgeDoc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

/**
 * RAG 知识文档数据访问
 */
@Repository
public class KnowledgeRepository {

    private static final RowMapper<KnowledgeDoc> ROW_MAPPER = (rs, i) -> new KnowledgeDoc(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("category"),
            rs.getString("content"),
            rs.getString("doc_source"));

    private final JdbcTemplate jdbc;

    public KnowledgeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<KnowledgeDoc> findAll() {
        return jdbc.query("SELECT id, title, category, content, doc_source FROM knowledge_doc ORDER BY id",
                ROW_MAPPER);
    }

    public Optional<KnowledgeDoc> findById(Long id) {
        return jdbc.query("SELECT id, title, category, content, doc_source FROM knowledge_doc WHERE id = ?",
                ROW_MAPPER, id).stream().findFirst();
    }

    public long save(KnowledgeDoc doc) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO knowledge_doc (title, category, content, doc_source) VALUES (?,?,?,?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, doc.title());
            ps.setString(2, doc.category());
            ps.setString(3, doc.content());
            ps.setString(4, doc.docSource());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? -1 : key.longValue();
    }

    public int update(KnowledgeDoc doc) {
        return jdbc.update("UPDATE knowledge_doc SET title=?, category=?, content=?, doc_source=? WHERE id=?",
                doc.title(), doc.category(), doc.content(), doc.docSource(), doc.id());
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM knowledge_doc WHERE id = ?", id);
    }
}
