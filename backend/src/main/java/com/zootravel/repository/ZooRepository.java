package com.zootravel.repository;

import com.zootravel.dto.ZooSaveRequest;
import com.zootravel.entity.Zoo;
import com.zootravel.entity.ZooAnimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 动物园数据访问（JdbcTemplate）
 */
@Repository
public class ZooRepository {

    private static final String BASE_COLUMNS =
            "z.id, z.name, z.city, z.province, z.address, z.longitude, z.latitude, " +
            "z.ticket_adult, z.ticket_child, z.ticket_note, z.open_time, z.open_hours_note, " +
            "z.website, z.description, z.star_animals";

    private static final RowMapper<Zoo> ROW_MAPPER = (rs, i) -> new Zoo(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("city"),
            rs.getString("province"),
            rs.getString("address"),
            rs.getBigDecimal("longitude"),
            rs.getBigDecimal("latitude"),
            rs.getBigDecimal("ticket_adult"),
            rs.getBigDecimal("ticket_child"),
            rs.getString("ticket_note"),
            rs.getString("open_time"),
            rs.getString("open_hours_note"),
            rs.getString("website"),
            rs.getString("description"),
            rs.getString("star_animals"));

    private final JdbcTemplate jdbc;

    public ZooRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Zoo> findById(Long id) {
        List<Zoo> list = jdbc.query("SELECT " + BASE_COLUMNS + " FROM zoo z WHERE z.id = ?",
                ROW_MAPPER, id);
        return list.stream().findFirst();
    }

    public List<Zoo> findAll() {
        return jdbc.query("SELECT " + BASE_COLUMNS + " FROM zoo z ORDER BY z.id", ROW_MAPPER);
    }

    public List<String> listCities() {
        return jdbc.queryForList(
                "SELECT DISTINCT city FROM zoo ORDER BY city", String.class);
    }

    public List<String> listAnimalNames() {
        return jdbc.queryForList(
                "SELECT DISTINCT name FROM animal ORDER BY name", String.class);
    }

    /**
     * 组合筛选分页：city 支持城市名/动物园名模糊，animal 支持动物名模糊
     */
    public List<Zoo> findPage(String city, String animal, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT " + BASE_COLUMNS + " FROM zoo z WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (city != null && !city.isBlank()) {
            sql.append(" AND (z.city LIKE ? OR z.name LIKE ?)");
            args.add("%" + city + "%");
            args.add("%" + city + "%");
        }
        if (animal != null && !animal.isBlank()) {
            sql.append(" AND EXISTS (SELECT 1 FROM zoo_animal za JOIN animal a ON a.id = za.animal_id" +
                    " WHERE za.zoo_id = z.id AND (a.name LIKE ? OR za.exhibit_area LIKE ?))");
            args.add("%" + animal + "%");
            args.add("%" + animal + "%");
        }
        sql.append(" ORDER BY z.id LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), ROW_MAPPER, args.toArray());
    }

    public long countSearch(String city, String animal) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM zoo z WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (city != null && !city.isBlank()) {
            sql.append(" AND (z.city LIKE ? OR z.name LIKE ?)");
            args.add("%" + city + "%");
            args.add("%" + city + "%");
        }
        if (animal != null && !animal.isBlank()) {
            sql.append(" AND EXISTS (SELECT 1 FROM zoo_animal za JOIN animal a ON a.id = za.animal_id" +
                    " WHERE za.zoo_id = z.id AND (a.name LIKE ? OR za.exhibit_area LIKE ?))");
            args.add("%" + animal + "%");
            args.add("%" + animal + "%");
        }
        Long count = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return Objects.requireNonNullElse(count, 0L);
    }

    /**
     * 供聊天用的宽松检索：city/name/animal 任一命中
     */
    public List<Zoo> searchForChat(String keyword) {
        String like = "%" + keyword + "%";
        return jdbc.query(
                "SELECT " + BASE_COLUMNS + " FROM zoo z WHERE z.city LIKE ? OR z.name LIKE ? OR z.province LIKE ?" +
                        " OR z.star_animals LIKE ?" +
                        " OR EXISTS (SELECT 1 FROM zoo_animal za JOIN animal a ON a.id = za.animal_id" +
                        " WHERE za.zoo_id = z.id AND a.name LIKE ?) ORDER BY z.id LIMIT 5",
                ROW_MAPPER, like, like, like, like, like);
    }

    public List<ZooAnimal> findAnimalsByZooId(Long zooId) {
        return jdbc.query(
                "SELECT za.id, za.zoo_id, za.animal_id, a.name AS animal_name, a.category, za.exhibit_area, " +
                        "za.show_time, za.is_star, za.note, a.fun_facts " +
                        "FROM zoo_animal za JOIN animal a ON a.id = za.animal_id " +
                        "WHERE za.zoo_id = ? ORDER BY za.is_star DESC, a.id",
                (rs, i) -> new ZooAnimal(
                        rs.getLong("id"),
                        rs.getLong("zoo_id"),
                        rs.getLong("animal_id"),
                        rs.getString("animal_name"),
                        rs.getString("category"),
                        rs.getString("exhibit_area"),
                        rs.getString("show_time"),
                        rs.getInt("is_star") == 1,
                        rs.getString("note"),
                        rs.getString("fun_facts")),
                zooId);
    }

    public long save(ZooSaveRequest req) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO zoo (name, city, province, address, longitude, latitude, ticket_adult, " +
                            "ticket_child, ticket_note, open_time, open_hours_note, website, description, star_animals) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setString(1, req.name());
            ps.setString(2, req.city());
            ps.setString(3, req.province());
            ps.setString(4, req.address());
            ps.setBigDecimal(5, req.longitude());
            ps.setBigDecimal(6, req.latitude());
            ps.setBigDecimal(7, req.ticketAdult());
            ps.setBigDecimal(8, req.ticketChild());
            ps.setString(9, req.ticketNote());
            ps.setString(10, req.openTime());
            ps.setString(11, req.openHoursNote());
            ps.setString(12, req.website());
            ps.setString(13, req.description());
            ps.setString(14, req.starAnimals());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? -1 : key.longValue();
    }

    public int update(Long id, ZooSaveRequest req) {
        return jdbc.update(
                "UPDATE zoo SET name=?, city=?, province=?, address=?, longitude=?, latitude=?, " +
                        "ticket_adult=?, ticket_child=?, ticket_note=?, open_time=?, open_hours_note=?, " +
                        "website=?, description=?, star_animals=? WHERE id=?",
                req.name(), req.city(), req.province(), req.address(), req.longitude(), req.latitude(),
                req.ticketAdult(), req.ticketChild(), req.ticketNote(), req.openTime(), req.openHoursNote(),
                req.website(), req.description(), req.starAnimals(), id);
    }

    public int delete(Long id) {
        jdbc.update("DELETE FROM zoo_animal WHERE zoo_id = ?", id);
        return jdbc.update("DELETE FROM zoo WHERE id = ?", id);
    }
}
