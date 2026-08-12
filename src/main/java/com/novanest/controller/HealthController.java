package com.novanest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Nova Nest Backend",
            "seederError", com.novanest.config.DataSeeder.lastError != null ? com.novanest.config.DataSeeder.lastError : "None"
        ));
    }

    @GetMapping("/seed")
    public ResponseEntity<Map<String, String>> seedData(org.springframework.beans.factory.annotation.Autowired com.novanest.config.DataSeeder dataSeeder) {
        try {
            dataSeeder.run();
            return ResponseEntity.ok(Map.of(
                "status", "Seeding completed",
                "seederError", com.novanest.config.DataSeeder.lastError != null ? com.novanest.config.DataSeeder.lastError : "None"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "Seeding failed", "error", e.getMessage()));
        }
    }
}
