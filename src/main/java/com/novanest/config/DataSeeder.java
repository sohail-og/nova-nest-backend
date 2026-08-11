package com.novanest.config;

import com.novanest.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataSeeder(ProductRepository productRepository, JdbcTemplate jdbcTemplate) {
        this.productRepository = productRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            log.info("Products table is empty. Executing seed.sql to populate initial data...");
            try (InputStream is = getClass().getResourceAsStream("/seed.sql")) {
                if (is != null) {
                    String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    String[] statements = sql.split(";");
                    
                    int catCount = 0;
                    int prodCount = 0;
                    int imgCount = 0;
                    
                    for (String statement : statements) {
                        String trimmed = statement.trim();
                        if (!trimmed.isEmpty()) {
                            try {
                                // If statement is an INSERT INTO, we can also modify it to INSERT IGNORE INTO
                                // to avoid duplicate key exceptions completely in MySQL.
                                if (trimmed.toUpperCase().startsWith("INSERT INTO")) {
                                    trimmed = trimmed.replaceFirst("(?i)INSERT INTO", "INSERT IGNORE INTO");
                                }
                                
                                int rowsAffected = jdbcTemplate.update(trimmed);
                                
                                if (rowsAffected > 0) {
                                    if (trimmed.toUpperCase().contains("INTO CATEGORIES")) {
                                        catCount++;
                                    } else if (trimmed.toUpperCase().contains("INTO PRODUCTS")) {
                                        prodCount++;
                                    } else if (trimmed.toUpperCase().contains("INTO PRODUCTIMAGES")) {
                                        imgCount++;
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("Skipping statement due to error: {} - {}", trimmed, e.getMessage());
                            }
                        }
                    }
                    log.info("Successfully executed seed.sql. Inserted {} categories, {} products, {} images.", catCount, prodCount, imgCount);
                } else {
                    log.warn("seed.sql not found in classpath!");
                }
            } catch (Exception e) {
                log.error("Failed to read seed.sql", e);
            }
        } else {
            log.info("Products table is already populated (count: {}). Skipping seed.sql.", productRepository.count());
        }
    }
}
