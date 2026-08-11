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
        Integer productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        
        if (productCount == null || productCount == 0) {
            log.info("Products table is empty. Executing seed.json to populate initial data...");
            try (InputStream is = getClass().getResourceAsStream("/seed.json")) {
                if (is != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(is);
                    
                    int catCount = 0;
                    int prodCount = 0;
                    int imgCount = 0;
                    
                    if (root.has("categories")) {
                        for (JsonNode cat : root.get("categories")) {
                            try {
                                int rows = jdbcTemplate.update("INSERT IGNORE INTO categories (category_id, category_name, category_image, description, display_order, visibility) VALUES (?, ?, ?, ?, 0, 1)",
                                        cat.get("id").asInt(),
                                        cat.get("categoryName").asText(),
                                        cat.hasNonNull("categoryImage") ? cat.get("categoryImage").asText() : null,
                                        cat.hasNonNull("description") ? cat.get("description").asText() : null
                                );
                                if (rows > 0) catCount++;
                            } catch (Exception e) {
                                log.warn("Error inserting category {}: {}", cat.get("id").asInt(), e.getMessage());
                                lastError = "Category error: " + e.getMessage();
                            }
                        }
                    }
                    
                    if (root.has("products")) {
                        for (JsonNode prod : root.get("products")) {
                            try {
                                int rows = jdbcTemplate.update("INSERT IGNORE INTO products (product_id, name, description, price, stock, category_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                                        prod.get("id").asInt(),
                                        prod.get("name").asText(),
                                        prod.hasNonNull("description") ? prod.get("description").asText() : null,
                                        new BigDecimal(prod.get("price").asText()),
                                        prod.get("stock").asInt(),
                                        prod.get("categoryId").asInt()
                                );
                                if (rows > 0) prodCount++;
                            } catch (Exception e) {
                                log.warn("Error inserting product {}: {}", prod.get("id").asInt(), e.getMessage());
                                lastError = "Product error: " + e.getMessage();
                            }
                        }
                    }
                    
                    if (root.has("images")) {
                        for (JsonNode img : root.get("images")) {
                            try {
                                int rows = jdbcTemplate.update("INSERT IGNORE INTO productimages (image_id, product_id, image_url) VALUES (?, ?, ?)",
                                        img.get("id").asInt(),
                                        img.get("productId").asInt(),
                                        img.get("imageUrl").asText()
                                );
                                if (rows > 0) imgCount++;
                            } catch (Exception e) {
                                log.warn("Error inserting image {}: {}", img.get("id").asInt(), e.getMessage());
                                lastError = "Image error: " + e.getMessage();
                            }
                        }
                    }
                    
                    log.info("Successfully executed seed.json. Inserted {} categories, {} products, {} images.", catCount, prodCount, imgCount);
                    if (catCount == 0 && prodCount == 0) {
                        lastError = "Parsed JSON but 0 rows inserted! Maybe they already exist but IDs differ?";
                    }
                } else {
                    log.warn("seed.json not found in classpath!");
                    lastError = "seed.json not found!";
                }
            } catch (Exception e) {
                log.error("Failed to read seed.json", e);
                lastError = "Failed to read seed.json: " + e.getMessage();
            }
        } else {
            log.info("Products table is already populated (count: {}). Skipping seed.json.", productCount);
        }
    }
}
