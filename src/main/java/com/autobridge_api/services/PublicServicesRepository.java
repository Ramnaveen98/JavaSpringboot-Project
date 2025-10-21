package com.autobridge_api.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Read-only repository for public Services listing.
 * Does NOT depend on entities: stable against schema refactors.
 */
@Repository
public class PublicServicesRepository {
    private final JdbcTemplate jdbc;

    public PublicServicesRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Row projection aligned with your `services` table. */
    public static record Row(
            Long id,
            String name,
            String description,
            Integer durationMin,
            Double price,
            Boolean active
    ) {}

    public List<Row> listActive() {
        final String sql = """
            SELECT id, name, description, duration_min, price, active
            FROM services
            WHERE active = 1
            ORDER BY id
        """;
        return jdbc.query(sql, (rs, i) -> new Row(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                (Integer) rs.getObject("duration_min"),
                rs.getDouble("price"),
                rs.getBoolean("active")
        ));
    }
}
