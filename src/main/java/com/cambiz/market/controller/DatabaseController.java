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

    @PostMapping("/create-admin")
    public ResponseEntity<Map<String, Object>> createAdmin() {
        List<String> results = new ArrayList<>();
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'admin@cambiz.cm'", Integer.class);
            if (count != null && count > 0) {
                results.add("Admin user already exists");
            } else {
                String encodedPassword = passwordEncoder.encode("Admin@123");
                jdbcTemplate.update(
                    "INSERT INTO users (email, phone, password, first_name, last_name, user_type, status, " +
                    "account_type, commission_rate, wallet_balance, total_earned, referral_code, created_at, updated_at) " +
                    "VALUES ('admin@cambiz.cm', '670000000', ?, 'Admin', 'User', 'ADMIN', 'ACTIVE', " +
                    "'REGULAR', 0, 0, 0, 'ADMIN1', NOW(), NOW())", encodedPassword);
                results.add("Admin user created: admin@cambiz.cm / Admin@123");
            }
            return ResponseEntity.ok(Map.of("success", true, "results", results));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/create-admin-roles")
    public ResponseEntity<Map<String, Object>> createAdminRoles() {
        List<String> results = new ArrayList<>();
        try {
            jdbcTemplate.update("INSERT INTO roles (name, description) SELECT 'ROLE_ADMIN', 'Admin Role' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='ROLE_ADMIN')");
            results.add("ROLE_ADMIN created/verified");
            
            jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.email='admin@cambiz.cm' AND r.name='ROLE_ADMIN' AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id AND ur.role_id=r.id)");
            results.add("Admin role assigned to admin@cambiz.cm");
            
            return ResponseEntity.ok(Map.of("success", true, "results", results));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ✅ UPDATED: Enhanced product approval migration
    @PostMapping("/add-approval-columns")
    public ResponseEntity<Map<String, Object>> addApprovalColumns() {
        List<String> results = new ArrayList<>();
        try {
            // Add status column (replaces boolean is_approved for more flexibility)
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING_APPROVAL'");
            results.add("status column added (PENDING_APPROVAL, APPROVED, REJECTED)");
        } catch (Exception e) { 
            results.add("status: " + e.getMessage()); 
        }
        
        try {
            // Sync existing is_approved to new status
            jdbcTemplate.execute("UPDATE products SET status = 'APPROVED' WHERE is_approved = true AND status = 'PENDING_APPROVAL'");
            results.add("Synced existing approved products to APPROVED status");
        } catch (Exception e) { 
            results.add("status sync: " + e.getMessage()); 
        }
        
        try {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS is_approved BOOLEAN DEFAULT FALSE");
            results.add("is_approved column added");
        } catch (Exception e) { 
            results.add("is_approved: " + e.getMessage()); 
        }
        
        try {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS approved_by VARCHAR(255)");
            results.add("approved_by column added (stores admin email)");
        } catch (Exception e) { 
            results.add("approved_by: " + e.getMessage()); 
        }
        
        try {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP");
            results.add("approved_at column added");
        } catch (Exception e) { 
            results.add("approved_at: " + e.getMessage()); 
        }
        
        try {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS rejection_reason TEXT");
            results.add("rejection_reason column added");
        } catch (Exception e) { 
            results.add("rejection_reason: " + e.getMessage()); 
        }
        
        try {
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP");
            results.add("rejected_at column added");
        } catch (Exception e) { 
            results.add("rejected_at: " + e.getMessage()); 
        }
        
        try {
            // Add constraint for valid status values
            jdbcTemplate.execute("ALTER TABLE products DROP CONSTRAINT IF EXISTS chk_product_status");
            jdbcTemplate.execute("ALTER TABLE products ADD CONSTRAINT chk_product_status CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED'))");
            results.add("Product status constraint added");
        } catch (Exception e) { 
            results.add("constraint: " + e.getMessage()); 
        }
        
        return ResponseEntity.ok(Map.of("success", true, "results", results));
    }

    // ✅ NEW: Get pending approval products count
    @GetMapping("/pending-approval-count")
    public ResponseEntity<Map<String, Object>> getPendingApprovalCount() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE status = 'PENDING_APPROVAL'", Integer.class);
            return ResponseEntity.ok(Map.of("success", true, "count", count != null ? count : 0));
        } catch (Exception e) {
            // Fallback to is_approved if status column doesn't exist
            try {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM products WHERE is_approved = false OR is_approved IS NULL", Integer.class);
                return ResponseEntity.ok(Map.of("success", true, "count", count != null ? count : 0));
            } catch (Exception e2) {
                return ResponseEntity.ok(Map.of("success", false, "error", e2.getMessage()));
            }
        }
    }
}