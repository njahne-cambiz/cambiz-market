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

    @PostMapping("/migrate-transactions")
    public ResponseEntity<Map<String, Object>> migrateTransactions() {
        List<String> results = new ArrayList<>();
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id BIGSERIAL PRIMARY KEY,
                    transaction_ref VARCHAR(20) UNIQUE NOT NULL,
                    gateway_reference VARCHAR(50) UNIQUE,
                    order_id BIGINT NOT NULL,
                    buyer_id BIGINT NOT NULL,
                    seller_id BIGINT,
                    type VARCHAR(20) NOT NULL,
                    payment_method VARCHAR(30) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount DOUBLE PRECISION NOT NULL,
                    platform_fee DOUBLE PRECISION NOT NULL,
                    net_amount DOUBLE PRECISION NOT NULL,
                    description VARCHAR(255),
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
            """);
            results.add("transactions table created");
        } catch (Exception e) {
            results.add("transactions table: " + e.getMessage());
        }
        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_txn_buyer ON transactions(buyer_id)");
            results.add("idx_txn_buyer created");
        } catch (Exception e) {
            results.add("idx_txn_buyer: " + e.getMessage());
        }
        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_txn_seller ON transactions(seller_id)");
            results.add("idx_txn_seller created");
        } catch (Exception e) {
            results.add("idx_txn_seller: " + e.getMessage());
        }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/add-map-column")
    public ResponseEntity<Map<String, Object>> addMapColumn() {
        List<String> results = new ArrayList<>();
        try {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS min_acceptable_price DOUBLE PRECISION");
            results.add("min_acceptable_price column added");
        } catch (Exception e) {
            results.add("min_acceptable_price: " + e.getMessage());
        }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/create-orders-table")
    public ResponseEntity<Map<String, Object>> createOrdersTable() {
        List<String> results = new ArrayList<>();
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS persisted_orders (
                    order_id BIGINT PRIMARY KEY,
                    order_number VARCHAR(50) UNIQUE,
                    order_data TEXT,
                    status VARCHAR(20),
                    created_at TIMESTAMP DEFAULT NOW()
                )
            """);
            results.add("persisted_orders table created");
        } catch (Exception e) {
            results.add("persisted_orders: " + e.getMessage());
        }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }
}