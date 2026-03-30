package com.expensetracker.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("timestamp", Instant.now().toString());
        status.put("database", dbStatus());
        return ResponseEntity.ok(status);
    }

    private Map<String, Object> dbStatus() {
        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2); // 2 second timeout
            db.put("status", valid ? "UP" : "DOWN");
            db.put("product", conn.getMetaData().getDatabaseProductName());
        } catch (Exception ex) {
            log.error("DB health check failed: {}", ex.getMessage());
            db.put("status", "DOWN");
            db.put("error", ex.getMessage());
        }
        return db;
    }
}
