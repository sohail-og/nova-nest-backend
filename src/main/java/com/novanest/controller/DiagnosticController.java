package com.novanest.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DiagnosticController {
    private final JdbcTemplate jdbcTemplate;

    public DiagnosticController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/diagnostic/seed")
    public List<String> seedDatabase() {
        List<String> results = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/seed.sql")) {
            if (is == null) {
                results.add("seed.sql not found!");
                return results;
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String[] statements = sql.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        if (trimmed.toUpperCase().startsWith("INSERT INTO")) {
                            trimmed = trimmed.replaceFirst("(?i)INSERT INTO", "INSERT IGNORE INTO");
                        }
                        int rows = jdbcTemplate.update(trimmed);
                        results.add("SUCCESS (" + rows + "): " + trimmed);
                    } catch (Exception e) {
                        results.add("ERROR: " + e.getMessage() + " | SQL: " + trimmed);
                    }
                }
            }
        } catch (Exception e) {
            results.add("FATAL ERROR: " + e.getMessage());
        }
        return results;
    }
}
