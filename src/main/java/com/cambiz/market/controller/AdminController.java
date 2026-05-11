package com.cambiz.market.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cambiz.market.dto.AdminStatsDTO;
import com.cambiz.market.model.Product;
import com.cambiz.market.model.Transaction;
import com.cambiz.market.model.TransactionType;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.UserRepository;
import com.cambiz.market.service.OrderService;
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
    private OrderService orderService;

    @Autowired
    private TransactionService transactionService;

    // ========== DASHBOARD STATS ==========
    
    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStats() {
        List<User> allUsers = userRepository.findAll();
        List<Transaction> allTxns = transactionService.getAllTransactions();
        
        double totalRevenue = allTxns.stream()
                .filter(t -> t.getType() == TransactionType.PURCHASE)
                .mapToDouble(Transaction::getPlatformFee)
                .sum();

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

    // ========== USER MANAGEMENT ==========
    
    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = users.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("firstName", u.getFirstName());
            map.put("lastName", u.getLastName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("userType", u.getUserType().name());
            map.put("accountType", u.getAccountType());
            map.put("status", u.getStatus().name());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", userList));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long userId, @RequestParam String status) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        }
        try {
            user.setStatus(User.UserStatus.valueOf(status.toUpperCase()));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "User status updated to " + status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid status: " + status));
        }
    }

    // ========== SELLER MANAGEMENT ==========
    
    @GetMapping("/sellers")
    public ResponseEntity<?> getSellers() {
        List<User> sellers = userRepository.findAll().stream()
                .filter(u -> u.getUserType() == User.UserType.SELLER)
                .collect(Collectors.toList());
        List<Map<String, Object>> sellerList = sellers.stream().map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("firstName", u.getFirstName());
            map.put("lastName", u.getLastName());
            map.put("email", u.getEmail());
            map.put("businessName", u.getBusinessName());
            map.put("accountType", u.getAccountType());
            map.put("status", u.getStatus().name());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", sellerList));
    }

    @PutMapping("/sellers/{sellerId}/verify")
    public ResponseEntity<?> verifySeller(@PathVariable Long sellerId) {
        User seller = userRepository.findById(sellerId).orElse(null);
        if (seller == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Seller not found"));
        }
        seller.setAccountType("PREMIUM");
        userRepository.save(seller);
        return ResponseEntity.ok(Map.of("success", true, "message", "Seller verified as Premium"));
    }

    @PutMapping("/sellers/{sellerId}/revoke-premium")
    public ResponseEntity<?> revokePremiumSeller(@PathVariable Long sellerId) {
        User seller = userRepository.findById(sellerId).orElse(null);
        if (seller == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Seller not found"));
        }
        seller.setAccountType("REGULAR");
        userRepository.save(seller);
        return ResponseEntity.ok(Map.of("success", true, "message", "Premium status revoked"));
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
        
        for (Transaction t : allTxns) {
            if (t.getType() == TransactionType.PURCHASE) {
                String day = t.getCreatedAt().toLocalDate().toString();
                revenueByDay.merge(day, t.getPlatformFee(), Double::sum);
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "data", revenueByDay));
    }

    @GetMapping("/analytics/revenue")
    public ResponseEntity<?> getRevenueAnalytics(@RequestParam(defaultValue = "daily") String period) {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        
        Map<String, Double> revenueData = new LinkedHashMap<>();
        Map<String, Integer> orderCount = new LinkedHashMap<>();
        double totalRevenue = 0;
        int totalOrders = 0;
        
        java.time.format.DateTimeFormatter formatter;
        if ("monthly".equals(period)) {
            formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");
        } else {
            formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        }
        
        for (Transaction t : allTxns) {
            if (t.getType() == TransactionType.PURCHASE) {
                String key = t.getCreatedAt().format(formatter);
                revenueData.merge(key, t.getPlatformFee(), Double::sum);
                orderCount.merge(key, 1, Integer::sum);
                totalRevenue += t.getPlatformFee();
                totalOrders++;
            }
        }
        
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("revenueByPeriod", revenueData);
        analytics.put("ordersByPeriod", orderCount);
        analytics.put("totalRevenue", totalRevenue);
        analytics.put("totalOrders", totalOrders);
        analytics.put("period", period);
        
        return ResponseEntity.ok(Map.of("success", true, "data", analytics));
    }

    @GetMapping("/analytics/top-products")
    public ResponseEntity<?> getTopProducts() {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        
        Map<String, Double> productRevenue = new LinkedHashMap<>();
        Map<String, Integer> productSales = new LinkedHashMap<>();
        
        for (Transaction t : allTxns) {
            if (t.getType() == TransactionType.PURCHASE && t.getDescription() != null) {
                String productName = t.getDescription().replace("Payment for Order #", "Order #");
                productRevenue.merge(productName, t.getAmount(), Double::sum);
                productSales.merge(productName, 1, Integer::sum);
            }
        }
        
        List<Map<String, Object>> topProducts = productRevenue.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(e -> {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("name", e.getKey());
                p.put("revenue", e.getValue());
                p.put("sales", productSales.getOrDefault(e.getKey(), 0));
                return p;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of("success", true, "data", topProducts));
    }

    @GetMapping("/analytics/top-sellers")
    public ResponseEntity<?> getTopSellers() {
        List<Transaction> allTxns = transactionService.getAllTransactions();
        
        Map<Long, Double> sellerRevenue = new LinkedHashMap<>();
        Map<Long, Integer> sellerSales = new LinkedHashMap<>();
        Map<Long, String> sellerNames = new LinkedHashMap<>();
        
        for (Transaction t : allTxns) {
            if (t.getType() == TransactionType.PURCHASE && t.getSellerId() != null) {
                sellerRevenue.merge(t.getSellerId(), t.getNetAmount(), Double::sum);
                sellerSales.merge(t.getSellerId(), 1, Integer::sum);
                if (!sellerNames.containsKey(t.getSellerId())) {
                    User seller = userRepository.findById(t.getSellerId()).orElse(null);
                    sellerNames.put(t.getSellerId(), seller != null ? 
                        (seller.getBusinessName() != null ? seller.getBusinessName() : seller.getFirstName()) : "Unknown");
                }
            }
        }
        
        List<Map<String, Object>> topSellers = sellerRevenue.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .map(e -> {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("id", e.getKey());
                s.put("name", sellerNames.getOrDefault(e.getKey(), "Unknown"));
                s.put("revenue", e.getValue());
                s.put("sales", sellerSales.getOrDefault(e.getKey(), 0));
                return s;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of("success", true, "data", topSellers));
    }

    // ========== PRODUCT APPROVAL SYSTEM ==========
    
    @GetMapping("/products/pending")
    public ResponseEntity<?> getPendingProducts() {
        List<Product> pending = productRepository.findByIsApprovedFalseOrIsApprovedNull();
        List<Map<String, Object>> productList = pending.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("description", p.getDescription());
            map.put("price", p.getPrice());
            map.put("discountedPrice", p.getDiscountedPrice());
            map.put("imageUrls", p.getImageUrls());
            map.put("stockQuantity", p.getStockQuantity());
            map.put("productCondition", p.getProductCondition());
            map.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A");
            map.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : null);
            map.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown");
            map.put("sellerBusinessName", p.getSeller() != null ? p.getSeller().getBusinessName() : null);
            map.put("sellerId", p.getSellerId());
            map.put("sellerEmail", p.getSeller() != null ? p.getSeller().getEmail() : null);
            map.put("createdAt", p.getCreatedAt());
            map.put("isApproved", p.getIsApproved());
            map.put("status", p.getStatus() != null ? p.getStatus().name() : "PENDING_APPROVAL");
            map.put("rejectionReason", p.getRejectionReason());
            map.put("minAcceptablePrice", p.getMinAcceptablePrice());
            map.put("viewCount", p.getViewCount());
            map.put("isFeatured", p.getIsFeatured());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", productList, "count", productList.size()));
    }
    
    @GetMapping("/products/approved")
    public ResponseEntity<?> getApprovedProducts() {
        List<Product> approved = productRepository.findByIsApprovedTrue();
        List<Map<String, Object>> productList = approved.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("description", p.getDescription());
            map.put("price", p.getPrice());
            map.put("discountedPrice", p.getDiscountedPrice());
            map.put("imageUrls", p.getImageUrls());
            map.put("stockQuantity", p.getStockQuantity());
            map.put("productCondition", p.getProductCondition());
            map.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A");
            map.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : null);
            map.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown");
            map.put("sellerBusinessName", p.getSeller() != null ? p.getSeller().getBusinessName() : null);
            map.put("sellerId", p.getSellerId());
            map.put("sellerEmail", p.getSeller() != null ? p.getSeller().getEmail() : null);
            map.put("createdAt", p.getCreatedAt());
            map.put("approvedAt", p.getApprovedAt());
            map.put("approvedBy", p.getApprovedBy());
            map.put("isApproved", p.getIsApproved());
            map.put("status", p.getStatus() != null ? p.getStatus().name() : "APPROVED");
            map.put("rejectionReason", p.getRejectionReason());
            map.put("minAcceptablePrice", p.getMinAcceptablePrice());
            map.put("viewCount", p.getViewCount());
            map.put("isFeatured", p.getIsFeatured());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", productList, "count", productList.size()));
    }
    
    @GetMapping("/products/rejected")
    public ResponseEntity<?> getRejectedProducts() {
        List<Product> rejected = productRepository.findByIsApprovedFalseAndRejectionReasonNotNull();
        List<Map<String, Object>> productList = rejected.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("description", p.getDescription());
            map.put("price", p.getPrice());
            map.put("discountedPrice", p.getDiscountedPrice());
            map.put("imageUrls", p.getImageUrls());
            map.put("stockQuantity", p.getStockQuantity());
            map.put("productCondition", p.getProductCondition());
            map.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A");
            map.put("categoryId", p.getCategory() != null ? p.getCategory().getId() : null);
            map.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown");
            map.put("sellerBusinessName", p.getSeller() != null ? p.getSeller().getBusinessName() : null);
            map.put("sellerId", p.getSellerId());
            map.put("sellerEmail", p.getSeller() != null ? p.getSeller().getEmail() : null);
            map.put("createdAt", p.getCreatedAt());
            map.put("rejectedAt", p.getRejectedAt());
            map.put("isApproved", p.getIsApproved());
            map.put("status", p.getStatus() != null ? p.getStatus().name() : "REJECTED");
            map.put("rejectionReason", p.getRejectionReason());
            map.put("minAcceptablePrice", p.getMinAcceptablePrice());
            map.put("viewCount", p.getViewCount());
            map.put("isFeatured", p.getIsFeatured());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", productList, "count", productList.size()));
    }
    
    @GetMapping("/products/pending-count")
    public ResponseEntity<?> getPendingCount() {
        long count = productRepository.countByIsApprovedFalseOrIsApprovedNull();
        return ResponseEntity.ok(Map.of("success", true, "count", count));
    }
    
    @PutMapping("/products/{productId}/approve")
    public ResponseEntity<?> approveProduct(@PathVariable Long productId, HttpSession session) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Product not found"));
        }
        
        User admin = (User) session.getAttribute("user");
        String adminEmail = admin != null ? admin.getEmail() : "system@cambiz.cm";
        
        product.setIsApproved(true);
        product.setIsActive(true);
        product.setStatus(Product.ProductStatus.APPROVED);
        product.setApprovedAt(LocalDateTime.now());
        product.setApprovedBy(adminEmail);
        product.setRejectionReason(null);
        productRepository.save(product);
        
        return ResponseEntity.ok(Map.of("success", true, "message", "Product approved successfully", 
            "data", Map.of("id", product.getId(), "status", product.getStatus().name())));
    }
    
    @PutMapping("/products/{productId}/reject")
    public ResponseEntity<?> rejectProduct(@PathVariable Long productId, @RequestParam String reason) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Product not found"));
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Rejection reason is required"));
        }
        
        product.setIsApproved(false);
        product.setIsActive(false);
        product.setStatus(Product.ProductStatus.REJECTED);
        product.setRejectionReason(reason.trim());
        product.setRejectedAt(LocalDateTime.now());
        productRepository.save(product);
        
        return ResponseEntity.ok(Map.of("success", true, "message", "Product rejected", 
            "data", Map.of("id", product.getId(), "status", product.getStatus().name(), "reason", reason)));
    }
    
    @PostMapping("/products/batch-approve")
    public ResponseEntity<?> batchApproveProducts(@RequestBody List<Long> productIds, HttpSession session) {
        if (productIds == null || productIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No products selected"));
        }
        
        User admin = (User) session.getAttribute("user");
        String adminEmail = admin != null ? admin.getEmail() : "system@cambiz.cm";
        
        int approvedCount = 0;
        for (Long id : productIds) {
            Product product = productRepository.findById(id).orElse(null);
            if (product != null && (product.getIsApproved() == null || !product.getIsApproved())) {
                product.setIsApproved(true);
                product.setIsActive(true);
                product.setStatus(Product.ProductStatus.APPROVED);
                product.setApprovedAt(LocalDateTime.now());
                product.setApprovedBy(adminEmail);
                product.setRejectionReason(null);
                productRepository.save(product);
                approvedCount++;
            }
        }
        
        return ResponseEntity.ok(Map.of("success", true, 
            "message", approvedCount + " product(s) approved successfully", 
            "count", approvedCount));
    }

    // ========== PRODUCT MANAGEMENT (ADMIN) ==========
    
    @GetMapping("/products/all")
    public ResponseEntity<?> getAllProducts() {
        List<Product> allProducts = productRepository.findAll();
        List<Map<String, Object>> productList = allProducts.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("price", p.getPrice());
            map.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : "N/A");
            map.put("sellerName", p.getSeller() != null ? p.getSeller().getFirstName() + " " + p.getSeller().getLastName() : "Unknown");
            map.put("isApproved", p.getIsApproved());
            map.put("status", p.getStatus() != null ? p.getStatus().name() : "UNKNOWN");
            map.put("isActive", p.getIsActive());
            map.put("createdAt", p.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "data", productList, "count", productList.size()));
    }
    
    @PutMapping("/products/{productId}/delete")
    public ResponseEntity<?> deleteProduct(@PathVariable Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Product not found"));
        }
        product.setIsActive(false);
        productRepository.save(product);
        return ResponseEntity.ok(Map.of("success", true, "message", "Product deactivated"));
    }
    
    @PutMapping("/products/{productId}/restore")
    public ResponseEntity<?> restoreProduct(@PathVariable Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Product not found"));
        }
        product.setIsActive(true);
        productRepository.save(product);
        return ResponseEntity.ok(Map.of("success", true, "message", "Product restored"));
    }

    // ========== SYSTEM HEALTH ==========
    
    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("apiStatus", "RUNNING");
        health.put("databaseStatus", "CONNECTED");
        health.put("uptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000 + " seconds");
        health.put("totalMemory", totalMemory / (1024 * 1024) + " MB");
        health.put("freeMemory", freeMemory / (1024 * 1024) + " MB");
        health.put("usedMemory", usedMemory / (1024 * 1024) + " MB");
        health.put("maxMemory", maxMemory / (1024 * 1024) + " MB");
        health.put("memoryUsagePercent", Math.round((usedMemory * 100.0) / maxMemory));
        health.put("processors", runtime.availableProcessors());
        health.put("javaVersion", System.getProperty("java.version"));
        health.put("osName", System.getProperty("os.name"));
        health.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(Map.of("success", true, "data", health));
    }
}