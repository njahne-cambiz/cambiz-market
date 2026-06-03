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
    public ResponseEntity<Map<String, Object>> getColumns(@PathVariable String table) { try { return ResponseEntity.ok(Map.of("success", true, "table", table, "columns", jdbcTemplate.queryForList("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = ?", table))); } catch (Exception e) { return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage())); } }

    @PostMapping("/add-featured-columns")
    public ResponseEntity<Map<String, Object>> addFeaturedColumns() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS featured_until TIMESTAMP"); r.add("featured_until"); } catch (Exception e) { r.add("err: " + e.getMessage()); } try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS featured_payment_id BIGINT"); r.add("featured_payment_id"); } catch (Exception e) { r.add("err: " + e.getMessage()); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/migrate-transactions")
    public ResponseEntity<Map<String, Object>> migrateTransactions() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS transactions (id BIGSERIAL PRIMARY KEY, transaction_ref VARCHAR(20) UNIQUE NOT NULL, gateway_reference VARCHAR(50) UNIQUE, order_id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, seller_id BIGINT, type VARCHAR(20) NOT NULL, payment_method VARCHAR(30) NOT NULL, status VARCHAR(20) NOT NULL, amount DOUBLE PRECISION NOT NULL, platform_fee DOUBLE PRECISION NOT NULL, net_amount DOUBLE PRECISION NOT NULL, description VARCHAR(255), created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW())"); r.add("transactions table"); } catch (Exception e) { r.add("err: " + e.getMessage()); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/add-map-column")
    public ResponseEntity<Map<String, Object>> addMapColumn() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS min_acceptable_price DOUBLE PRECISION"); r.add("min_acceptable_price"); } catch (Exception e) { r.add("err: " + e.getMessage()); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/create-orders-table")
    public ResponseEntity<Map<String, Object>> createOrdersTable() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS persisted_orders (order_id BIGINT PRIMARY KEY, order_number VARCHAR(50) UNIQUE, order_data TEXT, status VARCHAR(20), created_at TIMESTAMP DEFAULT NOW())"); r.add("persisted_orders"); } catch (Exception e) { r.add("err: " + e.getMessage()); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/create-admin")
    public ResponseEntity<Map<String, Object>> createAdmin() { List<String> r = new ArrayList<>(); try { Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = 'admin@cambiz.cm'", Integer.class); if (c != null && c > 0) { r.add("Admin exists"); } else { jdbcTemplate.update("INSERT INTO users (email, phone, password, first_name, last_name, user_type, status, account_type, commission_rate, wallet_balance, total_earned, referral_code, created_at, updated_at) VALUES ('admin@cambiz.cm', '670000000', ?, 'Admin', 'User', 'ADMIN', 'ACTIVE', 'REGULAR', 0, 0, 0, 'ADMIN1', NOW(), NOW())", passwordEncoder.encode("Admin@123")); r.add("Admin created"); } } catch (Exception e) { return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage())); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/create-admin-roles")
    public ResponseEntity<Map<String, Object>> createAdminRoles() { List<String> r = new ArrayList<>(); try { jdbcTemplate.update("INSERT INTO roles (name, description) SELECT 'ROLE_ADMIN', 'Admin Role' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name='ROLE_ADMIN')"); jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.email='admin@cambiz.cm' AND r.name='ROLE_ADMIN' AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id AND ur.role_id=r.id)"); r.add("Roles done"); } catch (Exception e) { return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage())); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/add-approval-columns")
    public ResponseEntity<Map<String, Object>> addApprovalColumns() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING_APPROVAL'"); r.add("status"); } catch (Exception e) { r.add("err: " + e.getMessage()); } try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS is_approved BOOLEAN DEFAULT FALSE"); } catch (Exception e) {} try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS approved_by VARCHAR(255)"); } catch (Exception e) {} try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP"); } catch (Exception e) {} try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS rejection_reason TEXT"); } catch (Exception e) {} try { jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP"); } catch (Exception e) {} return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/add-category-active")
    public ResponseEntity<Map<String, Object>> addCategoryActive() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE"); r.add("is_active"); } catch (Exception e) { r.add("err: " + e.getMessage()); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/create-settings-table")
    public ResponseEntity<Map<String, Object>> createSettingsTable() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS platform_settings (id BIGSERIAL PRIMARY KEY, setting_key VARCHAR(100) UNIQUE NOT NULL, setting_value TEXT, description VARCHAR(255), created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW())"); r.add("settings table"); } catch (Exception e) { r.add("err: " + e.getMessage()); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/create-disputes-table")
    public ResponseEntity<Map<String, Object>> createDisputesTable() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS disputes (id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, seller_id BIGINT NOT NULL, amount DOUBLE PRECISION NOT NULL, reason TEXT NOT NULL, evidence TEXT, status VARCHAR(30) DEFAULT 'OPEN', resolution TEXT, resolved_by VARCHAR(255), created_at TIMESTAMP DEFAULT NOW(), updated_at TIMESTAMP DEFAULT NOW(), resolved_at TIMESTAMP)"); r.add("disputes table"); } catch (Exception e) { r.add("err: " + e.getMessage()); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/create-dispute-comments")
    public ResponseEntity<Map<String, Object>> createDisputeCommentsTable() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS dispute_comments (id BIGSERIAL PRIMARY KEY, dispute_id BIGINT NOT NULL, user_id BIGINT NOT NULL, user_name VARCHAR(255), user_type VARCHAR(20), message TEXT NOT NULL, created_at TIMESTAMP DEFAULT NOW())"); r.add("comments table"); } catch (Exception e) { r.add("err: " + e.getMessage()); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/add-review-moderation")
    public ResponseEntity<Map<String, Object>> addReviewModeration() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("ALTER TABLE reviews ADD COLUMN IF NOT EXISTS is_hidden BOOLEAN DEFAULT FALSE"); r.add("is_hidden"); } catch (Exception e) { r.add("err: " + e.getMessage()); } try { jdbcTemplate.execute("ALTER TABLE reviews ADD COLUMN IF NOT EXISTS is_flagged BOOLEAN DEFAULT FALSE"); r.add("is_flagged"); } catch (Exception e) { r.add("err: " + e.getMessage()); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    @PostMapping("/add-review-reply")
    public ResponseEntity<Map<String, Object>> addReviewReply() { List<String> r = new ArrayList<>(); try { jdbcTemplate.execute("ALTER TABLE reviews ADD COLUMN IF NOT EXISTS admin_reply TEXT"); r.add("admin_reply"); } catch (Exception e) { r.add("err: " + e.getMessage()); } try { jdbcTemplate.execute("ALTER TABLE reviews ADD COLUMN IF NOT EXISTS replied_at TIMESTAMP"); r.add("replied_at"); } catch (Exception e) { r.add("err: " + e.getMessage()); } return ResponseEntity.ok(Map.of("success", true, "results", r)); }

    // ========== SEARCH MIGRATION (GET + POST) ==========
    @GetMapping("/migrate-search")
    @PostMapping("/migrate-search")
    public ResponseEntity<Map<String, Object>> migrateSearch() { 
        List<String> r = new ArrayList<>(); 
        try { 
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm"); 
            r.add("pg_trgm extension"); 
        } catch (Exception e) { 
            r.add("pg_trgm err: " + e.getMessage()); 
        } 
        try { 
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS search_vector tsvector"); 
            r.add("search_vector column"); 
        } catch (Exception e) { 
            r.add("search_vector err: " + e.getMessage()); 
        } 
        try { 
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS brand VARCHAR(100)"); 
            r.add("brand column"); 
        } catch (Exception e) { 
            r.add("brand err: " + e.getMessage()); 
        } 
        try { 
            jdbcTemplate.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS free_delivery BOOLEAN DEFAULT false"); 
            r.add("free_delivery column"); 
        } catch (Exception e) { 
            r.add("free_delivery err: " + e.getMessage()); 
        } 
        try { 
            jdbcTemplate.execute("CREATE OR REPLACE FUNCTION update_product_search_vector() RETURNS TRIGGER AS $$ BEGIN NEW.search_vector := setweight(to_tsvector('english', COALESCE(NEW.name, '')), 'A') || setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B') || setweight(to_tsvector('english', COALESCE(NEW.brand, '')), 'C'); RETURN NEW; END; $$ LANGUAGE plpgsql"); 
            r.add("search function"); 
        } catch (Exception e) { 
            r.add("function err: " + e.getMessage()); 
        } 
        try { 
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS update_product_search_vector_trigger ON products"); 
            jdbcTemplate.execute("CREATE TRIGGER update_product_search_vector_trigger BEFORE INSERT OR UPDATE ON products FOR EACH ROW EXECUTE FUNCTION update_product_search_vector()"); 
            r.add("search trigger"); 
        } catch (Exception e) { 
            r.add("trigger err: " + e.getMessage()); 
        } 
        try { 
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_product_search ON products USING GIN(search_vector)"); 
            r.add("search index"); 
        } catch (Exception e) { 
            r.add("search index err: " + e.getMessage()); 
        } 
        try { 
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_product_name_trgm ON products USING GIN(name gin_trgm_ops)"); 
            r.add("trgm index"); 
        } catch (Exception e) { 
            r.add("trgm index err: " + e.getMessage()); 
        } 
        try { 
            int updated = jdbcTemplate.update("UPDATE products SET search_vector = setweight(to_tsvector('english', COALESCE(name, '')), 'A') || setweight(to_tsvector('english', COALESCE(description, '')), 'B') || setweight(to_tsvector('english', COALESCE(brand, '')), 'C') WHERE search_vector IS NULL"); 
            r.add("updated " + updated + " products"); 
        } catch (Exception e) { 
            r.add("update err: " + e.getMessage()); 
        } 
        return ResponseEntity.ok(Map.of("success", true, "results", r)); 
    }
}