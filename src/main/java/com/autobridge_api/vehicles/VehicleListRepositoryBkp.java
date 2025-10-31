
/*
package com.autobridge_api.vehicles;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class VehicleListRepositoryBkp {

    private final JdbcTemplate jdbc;

    public VehicleListRepositoryBkp(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Row projection for the list API
    public static final class VehicleRow {
        private final Long id;
        private final String title;
        private final String brand;
        private final Double price;
        private final String imageUrl;

        public VehicleRow(Long id, String title, String brand, Double price, String imageUrl) {
            this.id = id;
            this.title = title;
            this.brand = brand;
            this.price = price;
            this.imageUrl = imageUrl;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getBrand() { return brand; }
        public Double getPrice() { return price; }
        public String getImageUrl() { return imageUrl; }
    }

    private static final RowMapper<VehicleRow> ROW_MAPPER = new RowMapper<>() {
        @Override public VehicleRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new VehicleRow(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("brand"),
                    rs.getDouble("price"),
                    rs.getString("imageUrl")
            );
        }
    };

    private static final String BASE_SQL = """
        SELECT
          iv.id                               AS id,
          CONCAT(mk.name, ' ', md.name)       AS title,
          mk.name                             AS brand,
          iv.price                            AS price,
          iv.image_url                         AS imageUrl
        FROM inventory_vehicle iv
        JOIN vehicle_make  mk ON mk.id = iv.make_id
        JOIN vehicle_model md ON md.id = iv.model_id
        """;

    // List all vehicles (optionally only AVAILABLE – uncomment WHERE if you want the filter)
    public List<VehicleRow> listAllRows() {
        // If you want to filter: BASE_SQL + " WHERE iv.status = 'AVAILABLE' ORDER BY iv.id"
        String sql = BASE_SQL + " ORDER BY iv.id";
        return jdbc.query(sql, ROW_MAPPER);
    }
}


 */
