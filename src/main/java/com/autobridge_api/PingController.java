package com.autobridge_api;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PingController {

    private final JdbcTemplate jdbc;

    public PingController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("ok", "true");
    }

    @GetMapping("/health/db")
    public Map<String, Object> db() {
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        String db = jdbc.queryForObject("SELECT DATABASE()", String.class);
        return Map.of("ok", true, "select1", one, "database", db);
    }
}

