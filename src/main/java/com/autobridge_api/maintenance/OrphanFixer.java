// src/main/java/com/autobridge_api/maintenance/OrphanFixer.java
package com.autobridge_api.maintenance;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFixer implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    @Override @Transactional
    public void run(String... args) {
        int n = jdbc.update("""
      UPDATE service_request r
      LEFT JOIN inventory_vehicle v ON v.id = r.inventory_vehicle_id
      SET r.inventory_vehicle_id = NULL
      WHERE r.inventory_vehicle_id IS NOT NULL AND v.id IS NULL
    """);
        if (n > 0) log.warn("OrphanFixer: cleared {} dangling inventory_vehicle_id references", n);
    }
}
