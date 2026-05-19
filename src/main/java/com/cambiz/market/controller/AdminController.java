package com.cambiz.market.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private OrderService orderService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private PlatformSettingService settingService;
    @Autowired
    private DisputeRepository disputeRepository;
    @Autowired
    private DisputeCommentRepository commentRepository;
    @Autowired
    private ReviewRepository reviewRepository;

    // ========== DASHBOARD STATS ==========
    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStats() {
        List<User> allUsers = userRepository.findAll();
        List<Transaction> allTxns = transactionService.getAllTransactions();
        double totalRevenue = allTxns.stream().filter(t -> t.getType() == TransactionType.PURCHASE).mapToDouble(Transaction::getPlatformFee).sum();
        long pendingApprovals = productRepository.countByIsApprovedFalseOrIsApprovedNull();
        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalUsers(allUsers.size())
                .totalSellers(allUsers.stream().filter(u -> u.getUserType() == User.UserType.SELLER).count())
                .totalBuyers(allUsers.stream().filter(u -> u.getUserType() == User.UserType.BUYER).count())
                .totalOrders(orderService.getAllOrders().size())
                .totalProducts(productRepository.count())
                .premiumSellers(allUsers.stream().filter(u -> "PREMIUM".equals(u.getAccountType())).count())
                .totalRevenue(totalRevenue).pendingDisputes((int) pendingApprovals).build();
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }

    // ========== USER MANAGEMENT ==========
    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = users.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId()); map.put("firstName", u.getFirstName()); map.put("lastName", u.getLastName());
            map.put("email", u.getEmail()); map.put("phone", u.getPhone()); map.put("userType", u.getUserType().name());
            map.put("accountType", u.getAccountType()); map.put("status", u.getStatus().name()); map.put("createdAt", u.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", userList));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long userId, @RequestParam String status) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        try { user.setStatus(User.UserStatus.valueOf(status.toUpperCase())); userRepository.save(user); return ResponseEntity.ok(Map.of("success", true, "message", "User status updated to " + status)); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid status")); }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        try { userRepository.delete(user); return ResponseEntity.ok(Map.of("success", true, "message", "User deleted permanently")); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot delete user: " + e.getMessage())); }
    }

    // ========== SELLER MANAGEMENT ==========
    @GetMapping("/sellers")
    public ResponseEntity<?> getSellers() {
        List<User> sellers = userRepository.findAll().stream().filter(u -> u.getUserType() == User.UserType.SELLER).collect(Collectors.toList());
        List<Map<String, Object>> sellerList = sellers.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId()); map.put("firstName", u.getFirstName()); map.put("lastName", u.getLastName());
            map.put("email", u.getEmail()); map.put("businessName", u.getBusinessName()); map.put("accountType", u.getAccountType());
            map.put("status", u.getStatus().name()); map.put("createdAt", u.getCreatedAt()); return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", sellerList));
    }

    @PutMapping("/sellers/{sellerId}/verify")
    public ResponseEntity<?> verifySeller(@PathVariable Long sellerId) {
        User seller = userRepository.findById(sellerId).orElse(null);
        if (seller == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Seller not found"));
        seller.setAccountType("PREMIUM"); userRepository.save(seller);
        return ResponseEntity.ok(Map.of("success", true, "message", "Seller verified as Premium"));
    }

    @PutMapping("/sellers/{sellerId}/revoke-premium")
    public ResponseEntity<?> revokePremiumSeller(@PathVariable Long sellerId) {
        User seller = userRepository.findById(sellerId).orElse(null);
        if (seller == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Seller not found"));
        seller.setAccountType("REGULAR"); userRepository.save(seller);
        return ResponseEntity.ok(Map.of("success", true, "message", "Premium status revoked"));
    }

    // ========== CATEGORY MANAGEMENT ==========
    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        List<Map<String, Object>> list = categories.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId()); map.put("name", c.getNameEn()); map.put("active", c.getIsActive() != null ? c.getIsActive() : true);
            map.put("productCount", productRepository.countByCategoryId(c.getId())); return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    @PostMapping("/categories")
    public ResponseEntity<?> addCategory(@RequestBody Map<String, String> body) {
        Category cat = new Category(); cat.setNameEn(body.get("name")); cat.setIsActive(true); categoryRepository.save(cat);
        return ResponseEntity.ok(Map.of("success", true, "message", "Category added successfully"));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Category cat = categoryRepository.findById(id).orElse(null);
        if (cat == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Category not found"));
        if (body.containsKey("name")) cat.setNameEn(body.get("name"));
        if (body.containsKey("active")) cat.setIsActive(Boolean.parseBoolean(body.get("active")));
        categoryRepository.save(cat); return ResponseEntity.ok(Map.of("success", true, "message", "Category updated"));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try { categoryRepository.deleteById(id); return ResponseEntity.ok(Map.of("success", true, "message", "Category deleted")); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot delete: " + e.getMessage())); }
    }

    // ========== REVENUE & COMMISSION ==========
    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenueData() {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        double totalRevenue = allTxns.stream().filter(t -> t.getType() == TransactionType.PURCHASE).mapToDouble(Transaction::getAmount).sum();
        double totalFees = allTxns.stream().filter(t -> t.getType() == TransactionType.PURCHASE).mapToDouble(Transaction::getPlatformFee).sum();
        double totalPayouts = totalRevenue - totalFees;
        double commissionRate = settingService.getDouble("commission_rate", 5.0);
        Map<String, Object> revenue = new LinkedHashMap<>();
        revenue.put("totalRevenue", totalRevenue); revenue.put("totalFees", totalFees);
        revenue.put("totalPayouts", totalPayouts); revenue.put("commissionRate", commissionRate); revenue.put("transactionCount", allTxns.size());
        return ResponseEntity.ok(Map.of("success", true, "data", revenue));
    }

    // ========== SETTINGS ==========
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("siteName", settingService.get("site_name", "CamBiz Market"));
        settings.put("currency", settingService.get("currency", "XAF"));
        settings.put("commissionRate", settingService.getDouble("commission_rate", 5.0));
        settings.put("payoutThreshold", settingService.getDouble("payout_threshold", 10000.0));
        settings.put("maxProductsRegular", settingService.getInt("max_products_regular", 50));
        settings.put("maxProductsPremium", settingService.getInt("max_products_premium", 500));
        settings.put("maintenanceMode", settingService.getBoolean("maintenance_mode", false));
        settings.put("allowRegistration", settingService.getBoolean("allow_registration", true));
        return ResponseEntity.ok(Map.of("success", true, "data", settings));
    }

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
        return ResponseEntity.ok(Map.of("success", true, "message", "Settings saved successfully"));
    }

    // ========== ORDER MANAGEMENT ==========
    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() {
        var orders = orderService.getAllOrders();
        return ResponseEntity.ok(Map.of("success", true, "data", orders, "count", orders.size()));
    }

    // ========== TRANSACTION MANAGEMENT ==========
    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions() {
        var transactions = transactionService.getAllTransactions();
        transactions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return ResponseEntity.ok(Map.of("success", true, "data", transactions, "count", transactions.size()));
    }

    // ========== ANALYTICS ==========
    @GetMapping("/revenue-chart")
    public ResponseEntity<?> getRevenueChart() {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        Map<String, Double> revenueByDay = new LinkedHashMap<>();
        for (Transaction t : allTxns) { if (t.getType() == TransactionType.PURCHASE) { String day = t.getCreatedAt().toLocalDate().toString(); revenueByDay.merge(day, t.getPlatformFee(), Double::sum); } }
        return ResponseEntity.ok(Map.of("success", true, "data", revenueByDay));
    }

    @GetMapping("/analytics/revenue")
    public ResponseEntity<?> getRevenueAnalytics(@RequestParam(defaultValue = "daily") String period) {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        Map<String, Double> revenueData = new LinkedHashMap<>(); Map<String, Integer> orderCount = new LinkedHashMap<>();
        double totalRevenue = 0; int totalOrders = 0;
        java.time.format.DateTimeFormatter formatter = "monthly".equals(period) ? java.time.format.DateTimeFormatter.ofPattern("yyyy-MM") : java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Transaction t : allTxns) { if (t.getType() == TransactionType.PURCHASE) { String key = t.getCreatedAt().format(formatter); revenueData.merge(key, t.getPlatformFee(), Double::sum); orderCount.merge(key, 1, Integer::sum); totalRevenue += t.getPlatformFee(); totalOrders++; } }
        Map<String, Object> analytics = new LinkedHashMap<>(); analytics.put("revenueByPeriod", revenueData); analytics.put("ordersByPeriod", orderCount); analytics.put("totalRevenue", totalRevenue); analytics.put("totalOrders", totalOrders); analytics.put("period", period);
        return ResponseEntity.ok(Map.of("success", true, "data", analytics));
    }

    @GetMapping("/analytics/top-products")
    public ResponseEntity<?> getTopProducts() {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        Map<String, Double> productRevenue = new LinkedHashMap<>(); Map<String, Integer> productSales = new LinkedHashMap<>();
        for (Transaction t : allTxns) { if (t.getType() == TransactionType.PURCHASE && t.getDescription() != null) { String n = t.getDescription().replace("Payment for Order #", "Order #"); productRevenue.merge(n, t.getAmount(), Double::sum); productSales.merge(n, 1, Integer::sum); } }
        List<Map<String, Object>> top = productRevenue.entrySet().stream().sorted((a,b)->b.getValue().compareTo(a.getValue())).limit(10).map(e->{ Map<String,Object> p=new LinkedHashMap<>(); p.put("name",e.getKey()); p.put("revenue",e.getValue()); p.put("sales",productSales.getOrDefault(e.getKey(),0)); return p; }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", top));
    }

    @GetMapping("/analytics/top-sellers")
    public ResponseEntity<?> getTopSellers() {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        Map<Long, Double> sr = new LinkedHashMap<>(); Map<Long, Integer> ss = new LinkedHashMap<>(); Map<Long, String> sn = new LinkedHashMap<>();
        for (Transaction t : allTxns) { if (t.getType() == TransactionType.PURCHASE && t.getSellerId() != null) { sr.merge(t.getSellerId(), t.getNetAmount(), Double::sum); ss.merge(t.getSellerId(), 1, Integer::sum); if (!sn.containsKey(t.getSellerId())) { User s = userRepository.findById(t.getSellerId()).orElse(null); sn.put(t.getSellerId(), s != null ? (s.getBusinessName() != null ? s.getBusinessName() : s.getFirstName()) : "Unknown"); } } }
        List<Map<String, Object>> top = sr.entrySet().stream().sorted((a,b)->b.getValue().compareTo(a.getValue())).limit(10).map(e->{ Map<String,Object> s=new LinkedHashMap<>(); s.put("id",e.getKey()); s.put("name",sn.getOrDefault(e.getKey(),"Unknown")); s.put("revenue",e.getValue()); s.put("sales",ss.getOrDefault(e.getKey(),0)); return s; }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", top));
    }

    // ========== PRODUCT APPROVAL SYSTEM ==========
    @GetMapping("/products/pending")
    public ResponseEntity<?> getPendingProducts() {
        List<Product> pending = productRepository.findByIsApprovedFalseOrIsApprovedNull();
        List<Map<String, Object>> list = pending.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("id", p.getId()); m.put("name", p.getName()); m.put("price", p.getPrice()); m.put("imageUrls", p.getImageUrls()); m.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A"); m.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown"); m.put("sellerBusinessName", p.getSeller() != null ? p.getSeller().getBusinessName() : null); m.put("sellerId", p.getSellerId()); m.put("createdAt", p.getCreatedAt()); m.put("status", p.getStatus() != null ? p.getStatus().name() : "PENDING_APPROVAL"); m.put("rejectionReason", p.getRejectionReason()); return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", list, "count", list.size()));
    }

    @GetMapping("/products/approved")
    public ResponseEntity<?> getApprovedProducts() {
        List<Product> approved = productRepository.findByIsApprovedTrue();
        List<Map<String, Object>> list = approved.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("id", p.getId()); m.put("name", p.getName()); m.put("price", p.getPrice()); m.put("imageUrls", p.getImageUrls()); m.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A"); m.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown"); m.put("sellerBusinessName", p.getSeller() != null ? p.getSeller().getBusinessName() : null); m.put("createdAt", p.getCreatedAt()); m.put("status", p.getStatus() != null ? p.getStatus().name() : "APPROVED"); return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", list, "count", list.size()));
    }

    @GetMapping("/products/rejected")
    public ResponseEntity<?> getRejectedProducts() {
        List<Product> rejected = productRepository.findByIsApprovedFalseAndRejectionReasonNotNull();
        List<Map<String, Object>> list = rejected.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("id", p.getId()); m.put("name", p.getName()); m.put("price", p.getPrice()); m.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A"); m.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown"); m.put("createdAt", p.getCreatedAt()); m.put("status", p.getStatus() != null ? p.getStatus().name() : "REJECTED"); m.put("rejectionReason", p.getRejectionReason()); return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", list, "count", list.size()));
    }

    @GetMapping("/products/pending-count")
    public ResponseEntity<?> getPendingCount() { return ResponseEntity.ok(Map.of("success", true, "count", productRepository.countByIsApprovedFalseOrIsApprovedNull())); }

    @PutMapping("/products/{id}/approve")
    public ResponseEntity<?> approveProduct(@PathVariable Long id, HttpSession session) {
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Product not found"));
        p.setIsApproved(true); p.setIsActive(true); p.setStatus(Product.ProductStatus.APPROVED); p.setApprovedAt(LocalDateTime.now()); p.setApprovedBy("admin@cambiz.cm"); p.setRejectionReason(null); productRepository.save(p);
        return ResponseEntity.ok(Map.of("success", true, "message", "Product approved"));
    }

    @PutMapping("/products/{id}/reject")
    public ResponseEntity<?> rejectProduct(@PathVariable Long id, @RequestParam String reason) {
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Product not found"));
        if (reason == null || reason.trim().isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Reason required"));
        p.setIsApproved(false); p.setIsActive(false); p.setStatus(Product.ProductStatus.REJECTED); p.setRejectionReason(reason.trim()); p.setRejectedAt(LocalDateTime.now()); productRepository.save(p);
        return ResponseEntity.ok(Map.of("success", true, "message", "Product rejected"));
    }

    @PostMapping("/products/batch-approve")
    public ResponseEntity<?> batchApprove(@RequestBody List<Long> ids, HttpSession session) {
        if (ids == null || ids.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No products selected"));
        int count = 0;
        for (Long id : ids) { Product p = productRepository.findById(id).orElse(null); if (p != null && (p.getIsApproved() == null || !p.getIsApproved())) { p.setIsApproved(true); p.setIsActive(true); p.setStatus(Product.ProductStatus.APPROVED); p.setApprovedAt(LocalDateTime.now()); p.setApprovedBy("admin@cambiz.cm"); p.setRejectionReason(null); productRepository.save(p); count++; } }
        return ResponseEntity.ok(Map.of("success", true, "message", count + " approved", "count", count));
    }

    @GetMapping("/products/all")
    public ResponseEntity<?> getAllProducts() {
        List<Product> all = productRepository.findAll();
        List<Map<String, Object>> list = all.stream().map(p -> { Map<String, Object> m = new LinkedHashMap<>(); m.put("id", p.getId()); m.put("name", p.getName()); m.put("price", p.getPrice()); m.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A"); m.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown"); m.put("isApproved", p.getIsApproved()); m.put("status", p.getStatus() != null ? p.getStatus().name() : "UNKNOWN"); m.put("isActive", p.getIsActive()); m.put("createdAt", p.getCreatedAt()); return m; }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", list, "count", list.size()));
    }

    @PutMapping("/products/{id}/delete")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) { Product p = productRepository.findById(id).orElse(null); if (p == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Not found")); p.setIsActive(false); productRepository.save(p); return ResponseEntity.ok(Map.of("success", true, "message", "Product deactivated")); }

    // ========== SYSTEM HEALTH ==========
    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        Runtime rt = Runtime.getRuntime(); long total = rt.totalMemory(), free = rt.freeMemory(), used = total - free, max = rt.maxMemory();
        Map<String, Object> h = new LinkedHashMap<>(); h.put("apiStatus", "RUNNING"); h.put("databaseStatus", "CONNECTED"); h.put("uptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000 + " seconds"); h.put("totalMemory", total / (1024*1024) + " MB"); h.put("freeMemory", free / (1024*1024) + " MB"); h.put("usedMemory", used / (1024*1024) + " MB"); h.put("maxMemory", max / (1024*1024) + " MB"); h.put("memoryUsagePercent", Math.round((used * 100.0) / max)); h.put("processors", rt.availableProcessors()); h.put("javaVersion", System.getProperty("java.version")); h.put("osName", System.getProperty("os.name")); h.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(Map.of("success", true, "data", h));
    }

    // ========== DISPUTE SETTLEMENT ==========
    @GetMapping("/disputes")
    public ResponseEntity<?> getDisputes() {
        List<Dispute> disputes = disputeRepository.findByOrderByCreatedAtDesc();
        List<Map<String, Object>> list = disputes.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("id", d.getId()); m.put("orderId", d.getOrderId()); m.put("buyerId", d.getBuyerId()); m.put("sellerId", d.getSellerId());
            User buyer = userRepository.findById(d.getBuyerId()).orElse(null); User seller = userRepository.findById(d.getSellerId()).orElse(null);
            String buyerName = buyer != null ? (buyer.getFirstName() != null ? buyer.getFirstName() + " " + (buyer.getLastName() != null ? buyer.getLastName() : "") : buyer.getEmail()) : "User #" + d.getBuyerId();
            String sellerName = seller != null ? (seller.getBusinessName() != null ? seller.getBusinessName() : (seller.getFirstName() != null ? seller.getFirstName() + " " + (seller.getLastName() != null ? seller.getLastName() : "") : seller.getEmail())) : "User #" + d.getSellerId();
            m.put("buyerName", buyerName.trim()); m.put("sellerName", sellerName.trim()); m.put("amount", d.getAmount()); m.put("reason", d.getReason()); m.put("status", d.getStatus().name()); m.put("resolution", d.getResolution()); m.put("resolvedBy", d.getResolvedBy()); m.put("createdAt", d.getCreatedAt()); m.put("resolvedAt", d.getResolvedAt()); return m;
        }).collect(Collectors.toList());
        long openCount = disputeRepository.countByStatus(Dispute.DisputeStatus.OPEN) + disputeRepository.countByStatus(Dispute.DisputeStatus.UNDER_REVIEW);
        long resolvedCount = disputeRepository.countByStatus(Dispute.DisputeStatus.RESOLVED_RELEASED) + disputeRepository.countByStatus(Dispute.DisputeStatus.RESOLVED_REFUNDED);
        return ResponseEntity.ok(Map.of("success", true, "data", list, "openCount", openCount, "resolvedCount", resolvedCount));
    }

    @PutMapping("/disputes/{id}/review")
    public ResponseEntity<?> reviewDispute(@PathVariable Long id) { Dispute d = disputeRepository.findById(id).orElse(null); if (d == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Dispute not found")); d.setStatus(Dispute.DisputeStatus.UNDER_REVIEW); disputeRepository.save(d); return ResponseEntity.ok(Map.of("success", true, "message", "Dispute #" + id + " under review")); }

    @PutMapping("/disputes/{id}/resolve")
    public ResponseEntity<?> resolveDispute(@PathVariable Long id, @RequestParam String resolution, @RequestParam(required = false) String notes) { Dispute d = disputeRepository.findById(id).orElse(null); if (d == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Dispute not found")); d.setStatus("RELEASED".equals(resolution) ? Dispute.DisputeStatus.RESOLVED_RELEASED : Dispute.DisputeStatus.RESOLVED_REFUNDED); d.setResolution(notes != null ? notes : ("RELEASED".equals(resolution) ? "Funds released to seller" : "Refund issued to buyer")); d.setResolvedBy("admin@cambiz.cm"); d.setResolvedAt(LocalDateTime.now()); disputeRepository.save(d); return ResponseEntity.ok(Map.of("success", true, "message", "Dispute #" + id + " resolved")); }

    @PostMapping("/disputes")
    public ResponseEntity<?> fileDispute(@RequestBody Map<String, Object> body) { Dispute d = new Dispute(); d.setOrderId(Long.valueOf(body.get("orderId").toString())); d.setBuyerId(Long.valueOf(body.get("buyerId").toString())); d.setSellerId(Long.valueOf(body.get("sellerId").toString())); d.setAmount(Double.valueOf(body.get("amount").toString())); d.setReason(body.get("reason").toString()); if (body.containsKey("evidence")) d.setEvidence(body.get("evidence").toString()); d.setStatus(Dispute.DisputeStatus.OPEN); disputeRepository.save(d); return ResponseEntity.ok(Map.of("success", true, "message", "Dispute filed successfully")); }

    @GetMapping("/disputes/{id}/comments")
    public ResponseEntity<?> getDisputeComments(@PathVariable Long id) { List<DisputeComment> comments = commentRepository.findByDisputeIdOrderByCreatedAtAsc(id); return ResponseEntity.ok(Map.of("success", true, "data", comments)); }

    @PostMapping("/disputes/{id}/comments")
    public ResponseEntity<?> addDisputeComment(@PathVariable Long id, @RequestBody Map<String, String> body) { DisputeComment c = new DisputeComment(); c.setDisputeId(id); c.setUserId(Long.valueOf(body.getOrDefault("userId", "0"))); c.setUserName(body.getOrDefault("userName", "User")); c.setUserType(body.getOrDefault("userType", "BUYER")); c.setMessage(body.get("message")); commentRepository.save(c); return ResponseEntity.ok(Map.of("success", true, "message", "Comment added")); }

    // ========== REVIEWS MODERATION ==========
    @GetMapping("/reviews")
    public ResponseEntity<?> getAllReviews() {
        List<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> list = reviews.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("id", r.getId()); m.put("productId", r.getProduct() != null ? r.getProduct().getId() : null); m.put("productName", r.getProduct() != null ? r.getProduct().getName() : "N/A"); m.put("userId", r.getUser() != null ? r.getUser().getId() : null); m.put("userName", r.getUser() != null ? (r.getUser().getFirstName() != null ? r.getUser().getFirstName() : r.getUser().getEmail()) : "N/A"); m.put("rating", r.getRating()); m.put("comment", r.getComment()); m.put("isHidden", r.getIsHidden()); m.put("isFlagged", r.getIsFlagged()); m.put("createdAt", r.getCreatedAt()); return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    @PutMapping("/reviews/{id}/hide")
    public ResponseEntity<?> hideReview(@PathVariable Long id) { Review r = reviewRepository.findById(id).orElse(null); if (r == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Review not found")); r.setIsHidden(true); reviewRepository.save(r); return ResponseEntity.ok(Map.of("success", true, "message", "Review hidden")); }

    @PutMapping("/reviews/{id}/unhide")
    public ResponseEntity<?> unhideReview(@PathVariable Long id) { Review r = reviewRepository.findById(id).orElse(null); if (r == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Review not found")); r.setIsHidden(false); r.setIsFlagged(false); reviewRepository.save(r); return ResponseEntity.ok(Map.of("success", true, "message", "Review visible again")); }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) { reviewRepository.deleteById(id); return ResponseEntity.ok(Map.of("success", true, "message", "Review deleted permanently")); }
}