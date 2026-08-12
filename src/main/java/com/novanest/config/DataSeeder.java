package com.novanest.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final JdbcTemplate jdbcTemplate;
    
    public static String lastError = "None";

    public DataSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting DataSeeder from seed.sql...");
        try (InputStream is = getClass().getResourceAsStream("/seed.sql")) {
            if (is == null) {
                log.warn("seed.sql not found in classpath!");
                lastError = "seed.sql not found!";
                return;
            }

            String sql = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String[] statements = sql.split(";");

            int successCount = 0;
            int failCount = 0;
            StringBuilder errorLog = new StringBuilder();

            for (String statement : statements) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) continue;

                try {
                    jdbcTemplate.execute(trimmed);
                    successCount++;
                } catch (Exception e) {
                    if (e.getMessage() != null && (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("already exists"))) {
                        // Idempotent ignore
                    } else {
                        log.warn("Error executing SQL statement: {}\nReason: {}", trimmed, e.getMessage());
                        errorLog.append(e.getMessage()).append(" | ");
                        failCount++;
                    }
                }
            }

            log.info("DataSeeder finished. Success: {}, Failed: {}", successCount, failCount);
            if (failCount > 0) {
                lastError = "Failed statements: " + failCount + " Errors: " + errorLog.toString();
            } else {
                lastError = "Success: " + successCount + " statements executed.";
            }

        } catch (Exception e) {
            log.error("Failed to read or execute seed.sql", e);
            lastError = "Fatal error: " + e.getMessage();
        }
    }
}
