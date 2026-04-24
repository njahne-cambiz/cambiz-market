package com.cambiz.market.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/db")
public class DatabaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/columns/{table}")
    public ResponseEntity<Map<String, Object>> getColumns(@PathVariable String table) {
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ?", table);
            return ResponseEntity.ok(Map.of("success", true, "table", table, "columns", columns));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/add-featured-columns")
    public ResponseEntity<Map<String, Object>> addFeaturedColumns() {
        List<String> results = new ArrayList<>();
        try {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS featured_until TIMESTAMP");
            results.add("featured_until added");
        } catch (Exception e) {
            results.add("featured_until: " + e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS featured_payment_id BIGINT");
            results.add("featured_payment_id added");
        } catch (Exception e) {
            results.add("featured_payment_id: " + e.getMessage());
        }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }
}
