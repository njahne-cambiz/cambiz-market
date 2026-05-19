package com.cambiz.market.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/db")
public class DatabaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
        try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS featured_until TIMESTAMP"); results.add("featured_until added"); } catch (Exception e) { results.add("featured_until: " + e.getMessage()); }
        try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS featured_payment_id BIGINT"); results.add("featured_payment_id added"); } catch (Exception e) { results.add("featured_payment_id: " + e.getMessage()); }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/migrate-transactions")
    public ResponseEntity<Map<String, Object>> migrateTransactions() {
        List<String> results = new ArrayList<>();
        try { jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS transactions (id BIGSERIAL PRIMARY KEY, transaction_ref VARCHAR(20) UNIQUE NOT NULL, gateway_reference VARCHAR(50) UNIQUE, order_id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, seller_id BIGINT, type VARCHAR(20) NOT NULL, payment_method VARCHAR(30) NOT NULL, status VARCHAR(20) NOT NULL, amount DOUBLE PRECISION NOT NULL, platform_fee DOUBLE PRECISION NOT NULL, net_amount DOUBLE PRECISION NOT NULL, description VARCHAR(255), created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW())"); results.add("transactions table created"); } catch (Exception e) { results.add("transactions: " + e.getMessage()); }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/add-map-column")
    public ResponseEntity<Map<String, Object>> addMapColumn() {
        List<String> results = new ArrayList<>();
        try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS min_acceptable_price DOUBLE PRECISION"); results.add("min_acceptable_price added"); } catch (Exception e) { results.add("min_acceptable_price: " + e.getMessage()); }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/create-orders-table")
    public ResponseEntity<Map<String, Object>> createOrdersTable() {
        List<String> results = new ArrayList<>();
        try { jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS persisted_orders (order_id BIGINT PRIMARY KEY, order_number VARCHAR(50) UNIQUE, order_data TEXT, status VARCHAR(20), created_at TIMESTAMP DEFAULT NOW())"); results.add("persisted_orders table created"); } catch (Exception e) { results.add("persisted_orders: " + e.getMessage()); }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/create-admin")
    public ResponseEntity<Map<String, Object>> createAdmin() {
        List<String> results = new ArrayList<>();
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = 'admin@cambiz.cm'", Integer.class);
            if (count != null && count > 0) { results.add("Admin user already exists"); }
            else { String ep = passwordEncoder.encode("Admin@123"); jdbcTemplate.update("INSERT INTO users (email, phone, password, first_name, last_name, user_type, status, account_type, commission_rate, wallet_balance, total_earned, referral_code, created_at, updated_at) VALUES ('admin@cambiz.cm', '670000000', ?, 'Admin', 'User', 'ADMIN', 'ACTIVE', 'REGULAR', 0, 0, 0, 'ADMIN1', NOW(), NOW())", ep); results.add("Admin created"); }
        } catch (Exception e) { return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage())); }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/create-admin-roles")
    public ResponseEntity<Map<String, Object>> createAdminRoles() {
        List<String> results = new ArrayList<>();
        try { jdbcTemplate.update("INSERT INTO roles (name, description) SELECT 'ROLE_ADMIN', 'Admin Role' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='ROLE_ADMIN')"); results.add("ROLE_ADMIN created"); jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.email='admin@cambiz.cm' AND r.name='ROLE_ADMIN' AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id AND ur.role_id=r.id)"); results.add("Admin role assigned"); } catch (Exception e) { return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage())); }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/add-approval-columns")
    public ResponseEntity<Map<String, Object>> addApprovalColumns() {
        List<String> results = new ArrayList<>();
        try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING_APPROVAL'"); results.add("status column added"); } catch (Exception e) { results.add("status: " + e.getMessage()); }
        try { jdbcTemplate.execute("UPDATE products SET status = 'APPROVED' WHERE is_approved = true AND status = 'PENDING_APPROVAL'"); } catch (Exception e) {}
        try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS is_approved BOOLEAN DEFAULT FALSE"); } catch (Exception e) {}
        try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS approved_by VARCHAR(255)"); } catch (Exception e) {}
        try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP"); } catch (Exception e) {}
        try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS rejection_reason TEXT"); } catch (Exception e) {}
        try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP"); } catch (Exception e) {}
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/add-category-active")
    public ResponseEntity<Map<String, Object>> addCategoryActive() {
        List<String> results = new ArrayList<>();
        try { jdbcTemplate.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE"); results.add("is_active added"); } catch (Exception e) { results.add("is_active: " + e.getMessage()); }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/create-settings-table")
    public ResponseEntity<Map<String, Object>> createSettingsTable() {
        List<String> results = new ArrayList<>();
        try { jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS platform_settings (id BIGSERIAL PRIMARY KEY, setting_key VARCHAR(100) UNIQUE NOT NULL, setting_value TEXT, description VARCHAR(255), created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW())"); results.add("platform_settings table created"); } catch (Exception e) { results.add("table: " + e.getMessage()); }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/create-disputes-table")
    public ResponseEntity<Map<String, Object>> createDisputesTable() {
        List<String> results = new ArrayList<>();
        try { jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS disputes (id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, seller_id BIGINT NOT NULL, amount DOUBLE PRECISION NOT NULL, reason TEXT NOT NULL, evidence TEXT, status VARCHAR(30) DEFAULT 'OPEN', resolution TEXT, resolved_by VARCHAR(255), created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW(), resolved_at TIMESTAMP)"); results.add("disputes table created"); } catch (Exception e) { results.add("disputes: " + e.getMessage()); }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    @PostMapping("/create-dispute-comments")
    public ResponseEntity<Map<String, Object>> createDisputeCommentsTable() {
        List<String> results = new ArrayList<>();
        try { jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS dispute_comments (id BIGSERIAL PRIMARY KEY, dispute_id BIGINT NOT NULL, user_id BIGINT NOT NULL, user_name VARCHAR(255), user_type VARCHAR(20), message TEXT NOT NULL, created_at TIMESTAMP DEFAULT NOW())"); results.add("dispute_comments table created"); } catch (Exception e) { results.add("comments: " + e.getMessage()); }
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }
}