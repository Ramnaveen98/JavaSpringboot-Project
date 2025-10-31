// src/main/java/com/autobridge_api/maintenance/OrphanFixer.java
package com.autobridge_api.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrphanFixer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OrphanFixer.class);

    private final JdbcTemplate jdbc;

    public OrphanFixer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(String... args) {
        final String sql = """
            UPDATE service_request r
            LEFT JOIN inventory_vehicles v ON v.id = r.inventory_vehicle_id
            SET r.inventory_vehicle_id = NULL
            WHERE r.inventory_vehicle_id IS NOT NULL AND v.id IS NULL
            """;
        try {
            int n = jdbc.update(sql);
            if (n > 0) {
                log.warn("OrphanFixer: cleared {} dangling inventory_vehicle_id reference(s).", n);
            } else {
                log.info("OrphanFixer: no orphans found for service_request.inventory_vehicle_id.");
            }
        } catch (Exception e) {
            log.error("OrphanFixer failed. Verify schema/migrations and DB connectivity.", e);
        }
    }
}
