package com.novanest.controller;

import com.novanest.config.DataSeeder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seed")
public class SeedController {

    private final DataSeeder dataSeeder;

    public SeedController(DataSeeder dataSeeder) {
        this.dataSeeder = dataSeeder;
    }

    @GetMapping
    public ResponseEntity<String> triggerSeed() {
        try {
            dataSeeder.run();
            return ResponseEntity.ok("Seeder execution completed.\nLast Result: " + DataSeeder.lastError);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to run seeder: " + e.getMessage());
        }
    }
}
