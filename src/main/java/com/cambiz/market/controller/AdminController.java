package com.cambiz.market.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.cambiz.market.dto.AdminStatsDTO;
import com.cambiz.market.model.Category;
import com.cambiz.market.model.Dispute;
import com.cambiz.market.model.DisputeComment;
import com.cambiz.market.model.Product;
import com.cambiz.market.model.Review;
import com.cambiz.market.model.Transaction;
import com.cambiz.market.model.TransactionType;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.CategoryRepository;
import com.cambiz.market.repository.DisputeCommentRepository;
import com.cambiz.market.repository.DisputeRepository;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.ReviewRepository;
import com.cambiz.market.repository.UserRepository;
import com.cambiz.market.service.OrderService;
import com.cambiz.market.service.PlatformSettingService;
import com.cambiz.market.service.TransactionService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private OrderService orderService;
    @Autowired private TransactionService transactionService;
    @Autowired private PlatformSettingService settingService;
    @Autowired private DisputeRepository disputeRepository;
    @Autowired private DisputeCommentRepository commentRepository;
    @Autowired private ReviewRepository reviewRepository; 

    // ========== DASHBOARD ==========
    @Transactional(readOnly = true)
    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStats() {
        List<User> allUsers = userRepository.findAll();
        List<Transaction> allTxns = transactionService.getAllTransactions();
        double totalRevenue = allTxns.stream()
            .filter(t -> t.getType() == TransactionType.PURCHASE)
            .mapToDouble(Transaction::getPlatformFee).sum();
        long pendingApprovals = productRepository.countByIsApprovedFalseOrIsApprovedNull();
        
        AdminStatsDTO stats = AdminStatsDTO.builder()
            .totalUsers(allUsers.size())
            .totalSellers(allUsers.stream().filter(u -> u.getUserType() == User.UserType.SELLER).count())
            .totalBuyers(allUsers.stream().filter(u -> u.getUserType() == User.UserType.BUYER).count())
            .totalOrders(orderService.getAllOrders().size())
            .totalProducts(productRepository.count())
            .premiumSellers(allUsers.stream().filter(u -> "PREMIUM".equals(u.getAccountType())).count())
            .totalRevenue(totalRevenue)
            .pendingDisputes((int) pendingApprovals)
            .build();
            
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }

    // ========== USERS ==========
    @Transactional(readOnly = true)
    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> list = users.stream().map(u -> { 
            Map<String, Object> m = new LinkedHashMap<>(); 
            m.put("id", u.getId()); 
            m.put("firstName", u.getFirstName()); 
            m.put("lastName", u.getLastName()); 
            m.put("email", u.getEmail()); 
            m.put("phone", u.getPhone()); 
            m.put("userType", u.getUserType().name()); 
            m.put("accountType", u.getAccountType()); 
            m.put("status", u.getStatus().name()); 
            m.put("createdAt", u.getCreatedAt()); 
            return m; 
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }  

    @Transactional
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long userId, @RequestParam String status) { 
        User u = userRepository.findById(userId).orElse(null); 
        if (u == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found")); 
        try { 
            u.setStatus(User.UserStatus.valueOf(status.toUpperCase())); 
            userRepository.save(u); 
            return ResponseEntity.ok(Map.of("success", true, "message", "Status updated")); 
        } catch (IllegalArgumentException e) { 
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid status")); 
        } 
    }

    @Transactional
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) { 
        User u = userRepository.findById(userId).orElse(null); 
        if (u == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found")); 
        try { 
            userRepository.delete(u); 
            return ResponseEntity.ok(Map.of("success", true, "message", "User deleted")); 
        } catch (Exception e) { 
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); 
        } 
    }

    // ========== SELLERS ==========
    @Transactional(readOnly = true)
    @GetMapping("/sellers")
    public ResponseEntity<?> getSellers() { 
        List<User> sellers = userRepository.findAll().stream()
            .filter(u -> u.getUserType() == User.UserType.SELLER)
            .collect(Collectors.toList()); 
            
        List<Map<String, Object>> list = sellers.stream().map(u -> { 
            Map<String, Object> m = new LinkedHashMap<>(); 
            m.put("id", u.getId()); 
            m.put("firstName", u.getFirstName()); 
            m.put("lastName", u.getLastName()); 
            m.put("email", u.getEmail()); 
            m.put("businessName", u.getBusinessName()); 
            m.put("accountType", u.getAccountType()); 
            m.put("status", u.getStatus().name()); 
            m.put("createdAt", u.getCreatedAt()); 
            return m; 
        }).collect(Collectors.toList()); 
        return ResponseEntity.ok(Map.of("success", true, "data", list)); 
    }

    @Transactional
    @PutMapping("/sellers/{sellerId}/verify")
    public ResponseEntity<?> verifySeller(@PathVariable Long sellerId) { 
        User s = userRepository.findById(sellerId).orElse(null); 
        if (s == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Seller not found")); 
        s.setAccountType("PREMIUM"); 
        userRepository.save(s); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Seller verified as Premium")); 
    }

    @Transactional
    @PutMapping("/sellers/{sellerId}/revoke-premium")
    public ResponseEntity<?> revokePremiumSeller(@PathVariable Long sellerId) { 
        User s = userRepository.findById(sellerId).orElse(null); 
        if (s == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Seller not found")); 
        s.setAccountType("REGULAR"); 
        userRepository.save(s); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Premium revoked")); 
    } 

    // ========== CATEGORIES ==========
    @Transactional(readOnly = true)
    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() { 
        List<Category> cats = categoryRepository.findAll(); 
        List<Map<String, Object>> list = cats.stream().map(c -> { 
            Map<String, Object> m = new LinkedHashMap<>(); 
            m.put("id", c.getId()); 
            m.put("name", c.getNameEn()); 
            m.put("active", c.getIsActive() != null ? c.getIsActive() : true); 
            m.put("productCount", productRepository.countByCategoryId(c.getId())); 
            return m; 
        }).collect(Collectors.toList()); 
        return ResponseEntity.ok(Map.of("success", true, "data", list)); 
    }

    @Transactional
    @PostMapping("/categories")
    public ResponseEntity<?> addCategory(@RequestBody Map<String, String> body) { 
        Category c = new Category(); 
        c.setNameEn(body.get("name")); 
        c.setIsActive(true); 
        categoryRepository.save(c); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Category added")); 
    }

    @Transactional
    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Map<String, String> body) { 
        Category c = categoryRepository.findById(id).orElse(null); 
        if (c == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Not found")); 
        if (body.containsKey("name")) c.setNameEn(body.get("name")); 
        if (body.containsKey("active")) c.setIsActive(Boolean.parseBoolean(body.get("active"))); 
        categoryRepository.save(c); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Category updated")); 
    }

    @Transactional
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) { 
        try { 
            categoryRepository.deleteById(id); 
            return ResponseEntity.ok(Map.of("success", true, "message", "Category deleted")); 
        } catch (Exception e) { 
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); 
        } 
    }  

    // ========== REVENUE ==========
    @Transactional(readOnly = true)
    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenueData() { 
        List<Transaction> txns = transactionService.getAllTransactions(); 
        double rev = txns.stream().filter(t -> t.getType() == TransactionType.PURCHASE).mapToDouble(Transaction::getAmount).sum(); 
        double fees = txns.stream().filter(t -> t.getType() == TransactionType.PURCHASE).mapToDouble(Transaction::getPlatformFee).sum(); 
        
        Map<String, Object> r = new LinkedHashMap<>(); 
        r.put("totalRevenue", rev); 
        r.put("totalFees", fees); 
        r.put("totalPayouts", rev - fees); 
        r.put("commissionRate", settingService.getDouble("commission_rate", 5.0)); 
        r.put("transactionCount", txns.size()); 
        return ResponseEntity.ok(Map.of("success", true, "data", r)); 
    }

    // ========== PAYMENTS ==========
    @Transactional(readOnly = true)
    @GetMapping("/payments")
    public ResponseEntity<?> getAllPayments() {
        List<Transaction> txns = transactionService.getAllTransactions();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Transaction t : txns) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId()); 
            m.put("orderId", t.getOrderId()); 
            m.put("amount", t.getAmount());
            m.put("paymentMethod", t.getPaymentMethod()); 
            m.put("status", t.getStatus()); 
            m.put("createdAt", t.getCreatedAt());
            list.add(m);
        }
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    } 

    // ========== SETTINGS ==========
    @Transactional(readOnly = true)
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() { 
        Map<String, Object> s = new LinkedHashMap<>(); 
        s.put("siteName", settingService.get("site_name", "CamBiz Market")); 
        s.put("currency", settingService.get("currency", "XAF")); 
        s.put("commissionRate", settingService.getDouble("commission_rate", 5.0)); 
        s.put("payoutThreshold", settingService.getDouble("payout_threshold", 10000.0)); 
        s.put("maxProductsRegular", settingService.getInt("max_products_regular", 50)); 
        s.put("maxProductsPremium", settingService.getInt("max_products_premium", 500)); 
        s.put("maintenanceMode", settingService.getBoolean("maintenance_mode", false)); 
        s.put("allowRegistration", settingService.getBoolean("allow_registration", true)); 
        return ResponseEntity.ok(Map.of("success", true, "data", s)); 
    }

    @Transactional
    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Object> body) { 
        if (body.containsKey("siteName")) settingService.set("site_name", String.valueOf(body.get("siteName"))); 
        if (body.containsKey("currency")) settingService.set("currency", String.valueOf(body.get("currency"))); 
        if (body.containsKey("commissionRate")) settingService.set("commission_rate", String.valueOf(body.get("commissionRate"))); 
        if (body.containsKey("payoutThreshold")) settingService.set("payout_threshold", String.valueOf(body.get("payoutThreshold"))); 
        if (body.containsKey("maxProductsRegular")) settingService.set("max_products_regular", String.valueOf(body.get("maxProductsRegular"))); 
        if (body.containsKey("maxProductsPremium")) settingService.set("max_products_premium", String.valueOf(body.get("maxProductsPremium"))); 
        if (body.containsKey("maintenanceMode")) settingService.set("maintenance_mode", String.valueOf(body.get("maintenanceMode"))); 
        if (body.containsKey("allowRegistration")) settingService.set("allow_registration", String.valueOf(body.get("allowRegistration"))); 
        
        settingService.loadCache(); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Settings saved")); 
    }

    // ========== ORDERS ==========
    @Transactional(readOnly = true)
    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() { 
        var orders = orderService.getAllOrders(); 
        return ResponseEntity.ok(Map.of("success", true, "data", orders, "count", orders.size())); 
    }

    // ========== TRANSACTIONS ==========
    @Transactional(readOnly = true)
    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions() { 
        var txns = transactionService.getAllTransactions(); 
        txns.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())); 
        return ResponseEntity.ok(Map.of("success", true, "data", txns, "count", txns.size())); 
    } 

    // ========== ANALYTICS ==========
    @Transactional(readOnly = true)
    @GetMapping("/revenue-chart")
    public ResponseEntity<?> getRevenueChart() { 
        List<Transaction> txns = transactionService.getAllTransactions(); 
        Map<String, Double> data = new LinkedHashMap<>(); 
        for (Transaction t : txns) { 
            if (t.getType() == TransactionType.PURCHASE) { 
                String day = t.getCreatedAt().toLocalDate().toString(); 
                data.merge(day, t.getPlatformFee(), Double::sum); 
            } 
        } 
        return ResponseEntity.ok(Map.of("success", true, "data", data)); 
    }

    @Transactional(readOnly = true)
    @GetMapping("/analytics/revenue")
    public ResponseEntity<?> getRevenueAnalytics(@RequestParam(defaultValue = "daily") String period) { 
        List<Transaction> txns = transactionService.getAllTransactions(); 
        Map<String, Double> rd = new LinkedHashMap<>(); 
        Map<String, Integer> oc = new LinkedHashMap<>(); 
        double tr = 0; int to = 0; 
        java.time.format.DateTimeFormatter f = "monthly".equals(period) ? java.time.format.DateTimeFormatter.ofPattern("yyyy-MM") : java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"); 
        
        for (Transaction t : txns) { 
            if (t.getType() == TransactionType.PURCHASE) { 
                String k = t.getCreatedAt().format(f); 
                rd.merge(k, t.getPlatformFee(), Double::sum); 
                oc.merge(k, 1, Integer::sum); 
                tr += t.getPlatformFee(); to++; 
            } 
        } 
        
        Map<String, Object> a = new LinkedHashMap<>(); 
        a.put("revenueByPeriod", rd); 
        a.put("ordersByPeriod", oc); 
        a.put("totalRevenue", tr); 
        a.put("totalOrders", to); 
        a.put("period", period); 
        return ResponseEntity.ok(Map.of("success", true, "data", a)); 
    }

    @Transactional(readOnly = true)
    @GetMapping("/analytics/top-products")
    public ResponseEntity<?> getTopProducts() { 
        List<Transaction> txns = transactionService.getAllTransactions(); 
        Map<String, Double> pr = new LinkedHashMap<>(); 
        Map<String, Integer> ps = new LinkedHashMap<>(); 
        
        for (Transaction t : txns) { 
            if (t.getType() == TransactionType.PURCHASE && t.getDescription() != null) { 
                String n = t.getDescription().replace("Payment for Order #", "Order #"); 
                pr.merge(n, t.getAmount(), Double::sum); 
                ps.merge(n, 1, Integer::sum); 
            } 
        } 
        
        List<Map<String, Object>> top = pr.entrySet().stream()
            .sorted((a,b)->b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(e->{ 
                Map<String,Object> p=new LinkedHashMap<>(); 
                p.put("name",e.getKey()); 
                p.put("revenue",e.getValue()); 
                p.put("sales",ps.getOrDefault(e.getKey(),0)); 
                return p; 
            }).collect(Collectors.toList()); 
            
        return ResponseEntity.ok(Map.of("success", true, "data", top)); 
    }

    @Transactional(readOnly = true)
    @GetMapping("/analytics/top-sellers")
    public ResponseEntity<?> getTopSellers() { 
        List<Transaction> txns = transactionService.getAllTransactions(); 
        Map<Long, Double> sr = new LinkedHashMap<>(); 
        Map<Long, Integer> ss = new LinkedHashMap<>(); 
        Map<Long, String> sn = new LinkedHashMap<>(); 
        
        for (Transaction t : txns) { 
            if (t.getType() == TransactionType.PURCHASE && t.getSellerId() != null) { 
                sr.merge(t.getSellerId(), t.getNetAmount(), Double::sum); 
                ss.merge(t.getSellerId(), 1, Integer::sum); 
                if (!sn.containsKey(t.getSellerId())) { 
                    User s = userRepository.findById(t.getSellerId()).orElse(null); 
                    sn.put(t.getSellerId(), s != null ? (s.getBusinessName() != null ? s.getBusinessName() : s.getFirstName()) : "Unknown"); 
                } 
            } 
        } 
        
        List<Map<String, Object>> top = sr.entrySet().stream()
            .sorted((a,b)->b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(e->{ 
                Map<String,Object> s=new LinkedHashMap<>(); 
                s.put("id",e.getKey()); 
                s.put("name",sn.getOrDefault(e.getKey(),"Unknown")); 
                s.put("revenue",e.getValue()); 
                s.put("sales",ss.getOrDefault(e.getKey(),0)); 
                return s; 
            }).collect(Collectors.toList()); 
            
        return ResponseEntity.ok(Map.of("success", true, "data", top)); 
    }  

    // ========== PRODUCTS ==========
    @Transactional(readOnly = true)
    @GetMapping("/products/pending")
    public ResponseEntity<?> getPendingProducts() { 
        List<Product> pending = productRepository.findByIsApprovedFalseOrIsApprovedNull(); 
        List<Map<String, Object>> list = new ArrayList<>(); 
        for (Product p : pending) { 
            Map<String, Object> m = new LinkedHashMap<>(); 
            m.put("id", p.getId()); 
            m.put("name", p.getName()); 
            m.put("price", p.getPrice()); 
            m.put("imageUrls", p.getImageUrls()); 
            m.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A"); 
            m.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown"); 
            m.put("sellerBusinessName", p.getSeller() != null ? p.getSeller().getBusinessName() : null); 
            m.put("sellerId", p.getSellerId()); 
            m.put("createdAt", p.getCreatedAt()); 
            m.put("status", p.getStatus() != null ? p.getStatus().name() : "PENDING_APPROVAL"); 
            m.put("rejectionReason", p.getRejectionReason()); 
            list.add(m); 
        } 
        return ResponseEntity.ok(Map.of("success", true, "data", list, "count", list.size())); 
    }

    @Transactional(readOnly = true)
    @GetMapping("/products/approved")
    public ResponseEntity<?> getApprovedProducts() { 
        List<Product> approved = productRepository.findByIsApprovedTrue(); 
        List<Map<String, Object>> list = new ArrayList<>(); 
        for (Product p : approved) { 
            Map<String, Object> m = new LinkedHashMap<>(); 
            m.put("id", p.getId()); 
            m.put("name", p.getName()); 
            m.put("price", p.getPrice()); 
            m.put("imageUrls", p.getImageUrls()); 
            m.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A"); 
            m.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown"); 
            m.put("sellerBusinessName", p.getSeller() != null ? p.getSeller().getBusinessName() : null); 
            m.put("createdAt", p.getCreatedAt()); 
            m.put("status", p.getStatus() != null ? p.getStatus().name() : "APPROVED"); 
            list.add(m); 
        } 
        return ResponseEntity.ok(Map.of("success", true, "data", list, "count", list.size())); 
    }

    @Transactional(readOnly = true)
    @GetMapping("/products/rejected")
    public ResponseEntity<?> getRejectedProducts() { 
        List<Product> rejected = productRepository.findByIsApprovedFalseAndRejectionReasonNotNull(); 
        List<Map<String, Object>> list = new ArrayList<>(); 
        for (Product p : rejected) { 
            Map<String, Object> m = new LinkedHashMap<>(); 
            m.put("id", p.getId()); 
            m.put("name", p.getName()); 
            m.put("price", p.getPrice()); 
            m.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A"); 
            m.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown"); 
            m.put("createdAt", p.getCreatedAt()); 
            m.put("status", p.getStatus() != null ? p.getStatus().name() : "REJECTED"); 
            m.put("rejectionReason", p.getRejectionReason()); 
            list.add(m); 
        } 
        return ResponseEntity.ok(Map.of("success", true, "data", list, "count", list.size())); 
    }

    @Transactional(readOnly = true)
    @GetMapping("/products/pending-count")
    public ResponseEntity<?> getPendingCount() { 
        return ResponseEntity.ok(Map.of("success", true, "count", productRepository.countByIsApprovedFalseOrIsApprovedNull())); 
    }

    @Transactional
    @PutMapping("/products/{id}/approve")
    public ResponseEntity<?> approveProduct(@PathVariable Long id) { 
        Product p = productRepository.findById(id).orElse(null); 
        if (p == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Not found")); 
        p.setIsApproved(true); 
        p.setIsActive(true); 
        p.setStatus(Product.ProductStatus.APPROVED); 
        p.setApprovedAt(LocalDateTime.now()); 
        p.setApprovedBy("admin@cambiz.cm"); 
        p.setRejectionReason(null); 
        productRepository.save(p); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Product approved")); 
    }

    @Transactional
    @PutMapping("/products/{id}/reject")
    public ResponseEntity<?> rejectProduct(@PathVariable Long id, @RequestParam String reason) { 
        Product p = productRepository.findById(id).orElse(null); 
        if (p == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Not found")); 
        if (reason == null || reason.trim().isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Reason required")); 
        p.setIsApproved(false); 
        p.setIsActive(false); 
        p.setStatus(Product.ProductStatus.REJECTED); 
        p.setRejectionReason(reason.trim()); 
        p.setRejectedAt(LocalDateTime.now()); 
        productRepository.save(p); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Product rejected")); 
    }  

    @Transactional
    @PostMapping("/products/batch-approve")
    public ResponseEntity<?> batchApprove(@RequestBody List<Long> ids) { 
        if (ids == null || ids.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No products")); 
        int count = 0; 
        for (Long id : ids) { 
            Product p = productRepository.findById(id).orElse(null); 
            if (p != null && (p.getIsApproved() == null || !p.getIsApproved())) { 
                p.setIsApproved(true); 
                p.setIsActive(true); 
                p.setStatus(Product.ProductStatus.APPROVED); 
                p.setApprovedAt(LocalDateTime.now()); 
                p.setApprovedBy("admin@cambiz.cm"); 
                productRepository.save(p); 
                count++; 
            } 
        } 
        return ResponseEntity.ok(Map.of("success", true, "message", count + " approved", "count", count)); 
    }

    @Transactional(readOnly = true)
    @GetMapping("/products/all")
    public ResponseEntity<?> getAllProducts() { 
        List<Product> all = productRepository.findAll(); 
        List<Map<String, Object>> list = new ArrayList<>(); 
        for (Product p : all) { 
            Map<String, Object> m = new LinkedHashMap<>(); 
            m.put("id", p.getId()); 
            m.put("name", p.getName()); 
            m.put("price", p.getPrice()); 
            m.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A"); 
            m.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown"); 
            m.put("isApproved", p.getIsApproved()); 
            m.put("status", p.getStatus() != null ? p.getStatus().name() : "UNKNOWN"); 
            m.put("isActive", p.getIsActive()); 
            m.put("createdAt", p.getCreatedAt()); 
            list.add(m); 
        } 
        return ResponseEntity.ok(Map.of("success", true, "data", list, "count", list.size())); 
    }

    @Transactional
    @PutMapping("/products/{id}/delete")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) { 
        Product p = productRepository.findById(id).orElse(null); 
        if (p == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Not found")); 
        p.setIsActive(false); 
        productRepository.save(p); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Product deactivated")); 
    }

    // ========== HEALTH ==========
    @Transactional(readOnly = true)
    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() { 
        Runtime rt = Runtime.getRuntime(); 
        long total = rt.totalMemory(), free = rt.freeMemory(), used = total - free, max = rt.maxMemory(); 
        Map<String, Object> h = new LinkedHashMap<>(); 
        h.put("apiStatus", "RUNNING"); 
        h.put("databaseStatus", "CONNECTED"); 
        h.put("uptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000 + " seconds"); 
        h.put("totalMemory", total / (1024*1024) + " MB"); 
        h.put("freeMemory", free / (1024*1024) + " MB"); 
        h.put("usedMemory", used / (1024*1024) + " MB"); 
        h.put("maxMemory", max / (1024*1024) + " MB"); 
        h.put("memoryUsagePercent", Math.round((used * 100.0) / max)); 
        h.put("processors", rt.availableProcessors()); 
        h.put("javaVersion", System.getProperty("java.version")); 
        h.put("osName", System.getProperty("os.name")); 
        h.put("timestamp", LocalDateTime.now().toString()); 
        return ResponseEntity.ok(Map.of("success", true, "data", h)); 
    }

    // ========== DISPUTES ==========
    @Transactional(readOnly = true)
    @GetMapping("/disputes")
    public ResponseEntity<?> getDisputes() { 
        List<Dispute> disputes = disputeRepository.findByOrderByCreatedAtDesc(); 
        List<Map<String, Object>> list = disputes.stream().map(d -> { 
            Map<String, Object> m = new LinkedHashMap<>(); 
            m.put("id", d.getId()); 
            m.put("orderId", d.getOrderId()); 
            m.put("buyerId", d.getBuyerId()); 
            m.put("sellerId", d.getSellerId()); 
            User buyer = userRepository.findById(d.getBuyerId()).orElse(null); 
            User seller = userRepository.findById(d.getSellerId()).orElse(null); 
            String bn = buyer != null ? (buyer.getFirstName() != null ? buyer.getFirstName() + " " + (buyer.getLastName() != null ? buyer.getLastName() : "") : buyer.getEmail()) : "User #" + d.getBuyerId(); 
            String sn = seller != null ? (seller.getBusinessName() != null ? seller.getBusinessName() : (seller.getFirstName() != null ? seller.getFirstName() + " " + (seller.getLastName() != null ? seller.getLastName() : "") : seller.getEmail())) : "User #" + d.getSellerId(); 
            m.put("buyerName", bn.trim()); 
            m.put("sellerName", sn.trim()); 
            m.put("amount", d.getAmount()); 
            m.put("reason", d.getReason()); 
            m.put("status", d.getStatus().name()); 
            m.put("resolution", d.getResolution()); 
            m.put("resolvedBy", d.getResolvedBy()); 
            m.put("createdAt", d.getCreatedAt()); 
            m.put("resolvedAt", d.getResolvedAt()); 
            return m; 
        }).collect(Collectors.toList()); 
        
        long open = disputeRepository.countByStatus(Dispute.DisputeStatus.OPEN) + disputeRepository.countByStatus(Dispute.DisputeStatus.UNDER_REVIEW); 
        long resolved = disputeRepository.countByStatus(Dispute.DisputeStatus.RESOLVED_RELEASED) + disputeRepository.countByStatus(Dispute.DisputeStatus.RESOLVED_REFUNDED); 
        return ResponseEntity.ok(Map.of("success", true, "data", list, "openCount", open, "resolvedCount", resolved)); 
    }     

    @Transactional
    @PutMapping("/disputes/{id}/resolve")
    public ResponseEntity<?> resolveDispute(@PathVariable Long id, @RequestParam String resolution, @RequestParam(required = false) String notes) { 
        Dispute d = disputeRepository.findById(id).orElse(null); 
        if (d == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Not found")); 
        d.setStatus("RELEASED".equals(resolution) ? Dispute.DisputeStatus.RESOLVED_RELEASED : Dispute.DisputeStatus.RESOLVED_REFUNDED); 
        d.setResolution(notes != null ? notes : ("RELEASED".equals(resolution) ? "Funds released to seller" : "Refund issued to buyer")); 
        d.setResolvedBy("admin@cambiz.cm"); 
        d.setResolvedAt(LocalDateTime.now()); 
        disputeRepository.save(d); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Dispute resolved")); 
    }

    @Transactional
    @PostMapping("/disputes")
    public ResponseEntity<?> fileDispute(@RequestBody Map<String, Object> body) { 
        Dispute d = new Dispute(); 
        d.setOrderId(Long.valueOf(body.get("orderId").toString())); 
        d.setBuyerId(Long.valueOf(body.get("buyerId").toString())); 
        d.setSellerId(Long.valueOf(body.get("sellerId").toString())); 
        d.setAmount(Double.valueOf(body.get("amount").toString())); 
        d.setReason(body.get("reason").toString()); 
        d.setStatus(Dispute.DisputeStatus.OPEN); 
        disputeRepository.save(d); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Dispute filed")); 
    }

    @Transactional(readOnly = true)
    @GetMapping("/disputes/{id}/comments")
    public ResponseEntity<?> getDisputeComments(@PathVariable Long id) { 
        return ResponseEntity.ok(Map.of("success", true, "data", commentRepository.findByDisputeIdOrderByCreatedAtAsc(id))); 
    }

    @Transactional
    @PostMapping("/disputes/{id}/comments")
    public ResponseEntity<?> addDisputeComment(@PathVariable Long id, @RequestBody Map<String, String> body) { 
        DisputeComment c = new DisputeComment(); 
        c.setDisputeId(id); 
        c.setUserId(Long.valueOf(body.getOrDefault("userId", "0"))); 
        c.setUserName(body.getOrDefault("userName", "User")); 
        c.setUserType(body.getOrDefault("userType", "BUYER")); 
        c.setMessage(body.get("message")); 
        commentRepository.save(c); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Comment added")); 
    }

    // ========== REVIEWS ==========
    @Transactional(readOnly = true)
    @GetMapping("/reviews")
    public ResponseEntity<?> getAllReviews() { 
        List<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc(); 
        List<Map<String, Object>> list = new ArrayList<>(); 
        for (Review r : reviews) { 
            Map<String, Object> m = new LinkedHashMap<>(); 
            m.put("id", r.getId()); 
            m.put("productName", r.getProduct() != null ? r.getProduct().getName() : "N/A"); 
            m.put("userName", r.getUser() != null ? (r.getUser().getFirstName() != null ? r.getUser().getFirstName() : r.getUser().getEmail()) : "N/A"); 
            m.put("rating", r.getRating()); 
            m.put("comment", r.getComment()); 
            m.put("isHidden", r.getIsHidden() != null ? r.getIsHidden() : false); 
            m.put("adminReply", r.getAdminReply()); 
            m.put("createdAt", r.getCreatedAt()); 
            list.add(m); 
        } 
        return ResponseEntity.ok(Map.of("success", true, "data", list)); 
    }

    @Transactional 
    @PutMapping("/reviews/{id}/hide") 
    public ResponseEntity<?> hideReview(@PathVariable Long id) { 
        Review r = reviewRepository.findById(id).orElse(null); 
        if (r == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Not found")); 
        r.setIsHidden(true); 
        reviewRepository.save(r); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Review hidden")); 
    }
    
    @Transactional 
    @PutMapping("/reviews/{id}/unhide") 
    public ResponseEntity<?> unhideReview(@PathVariable Long id) { 
        Review r = reviewRepository.findById(id).orElse(null); 
        if (r == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Not found")); 
        r.setIsHidden(false); 
        reviewRepository.save(r); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Review visible")); 
    }
    
    @Transactional 
    @DeleteMapping("/reviews/{id}") 
    public ResponseEntity<?> deleteReview(@PathVariable Long id) { 
        reviewRepository.deleteById(id); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Review deleted")); 
    }
    
    @Transactional 
    @PutMapping("/reviews/{id}/reply") 
    public ResponseEntity<?> replyToReview(@PathVariable Long id, @RequestBody Map<String, String> body) { 
        Review r = reviewRepository.findById(id).orElse(null); 
        if (r == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Not found")); 
        r.setAdminReply(body.get("reply")); 
        r.setRepliedAt(LocalDateTime.now()); 
        reviewRepository.save(r); 
        return ResponseEntity.ok(Map.of("success", true, "message", "Reply posted")); 
    }

    // =====================================================================
    // ============= NEW FEATURES INTEGRATION (DASHBOARD TABS) =============
    // =====================================================================

    // ========== PAYOUTS (Tab 8) ==========
    @Transactional(readOnly = true)
    @GetMapping("/payouts/pending")
    public ResponseEntity<?> getPendingVendorSettlements() {
        // Mocked implementation until PayoutRepository is created
        List<Map<String, Object>> mockPayouts = List.of(
            Map.of("payoutId", 302, "merchantId", 11, "destinationPhone", "677890123", "amountToSettle", 85000, "status", "PENDING")
        );
        return ResponseEntity.ok(Map.of("success", true, "data", mockPayouts));
    }

    // ========== FEATURED SLOTS (Tab 11) ==========
    @Transactional(readOnly = true)
    @GetMapping("/featured")
    public ResponseEntity<?> getActiveSponsoredSlots() {
        // Mocked implementation until FeaturedProductRepository is created
        List<Map<String, Object>> mockFeatured = List.of(
            Map.of("id", 701, "productId", 101, "featuredUntil", LocalDateTime.now().plusDays(10).toString(), "paymentId", 9811)
        );
        return ResponseEntity.ok(Map.of("success", true, "data", mockFeatured));
    }

    // ========== PREMIUM SUBSCRIPTIONS (Tab 12) ==========
    @Transactional(readOnly = true)
    @GetMapping("/premium/active")
    public ResponseEntity<?> getActivePremiumSellers() {
        List<User> premiumSellers = userRepository.findAll().stream()
            .filter(u -> "PREMIUM".equals(u.getAccountType()))
            .collect(Collectors.toList());
            
        List<Map<String, Object>> list = premiumSellers.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("businessName", u.getBusinessName() != null ? u.getBusinessName() : u.getFirstName());
            m.put("email", u.getEmail());
            m.put("premiumUntil", LocalDateTime.now().plusMonths(6).toString()); // Placeholder logic
            return m;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    // ========== KEYWORD ANALYTICS (Tab 15) ==========
    @Transactional(readOnly = true)
    @GetMapping("/analytics/keywords")
    public ResponseEntity<?> getSearchTermFrequencies() {
        // Mocked implementation until SearchAnalyticsRepository is created
        List<Map<String, Object>> mockKeywords = List.of(
            Map.of("term", "Shoes", "count", 890),
            Map.of("term", "Phones", "count", 1420),
            Map.of("term", "Laptops", "count", 650)
        );
        return ResponseEntity.ok(Map.of("success", true, "data", mockKeywords));
    }

    // ========== SECURITY LOGS (Tab 16) ==========
    @Transactional(readOnly = true)
    @GetMapping("/security/logs")
    public ResponseEntity<?> getRateLimiterLogs() {
        // Mocked implementation until SecurityLogRepository is created
        List<Map<String, Object>> mockLogs = List.of(
            Map.of("ip", "197.244.32.10", "endpoint", "/api/auth/login", "hits", "140 req/min", "action", "BLOCKED"),
            Map.of("ip", "41.202.64.189", "endpoint", "/api/products", "hits", "12 req/min", "action", "ALLOWED")
        );
        return ResponseEntity.ok(Map.of("success", true, "data", mockLogs));
    }

    // ========== AUDIT TRAIL (Tab 20) ==========
    @Transactional(readOnly = true)
    @GetMapping("/audit/logs")
    public ResponseEntity<?> getAdministrativeAuditTrail() {
        // Mocked implementation until AuditLogRepository is created
        List<Map<String, Object>> mockAudit = List.of(
            Map.of("timestamp", LocalDateTime.now().toString(), "adminContext", "SYSTEM_ROOT", "actionSignature", "PRODUCT_AUTO_FLUSH", "entityMutated", "Refreshed expired featured statuses")
        );
        return ResponseEntity.ok(Map.of("success", true, "data", mockAudit));
    }
}