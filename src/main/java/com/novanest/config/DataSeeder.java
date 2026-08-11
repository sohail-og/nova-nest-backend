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
                    int count = 0;
                    for (String statement : statements) {
                        if (!statement.trim().isEmpty()) {
                            jdbcTemplate.execute(statement.trim());
                            count++;
                        }
                    }
                    log.info("Successfully executed {} statements from seed.sql.", count);
                } else {
                    log.warn("seed.sql not found in classpath!");
                }
            } catch (Exception e) {
                log.error("Failed to execute seed.sql", e);
            }
        } else {
            log.info("Products table is already populated (count: {}). Skipping seed.sql.", productRepository.count());
        }
    }
}
