package com.cambiz.market.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cambiz.market.dto.AdminStatsDTO;
import com.cambiz.market.model.Transaction;
import com.cambiz.market.model.TransactionType;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.UserRepository;
import com.cambiz.market.service.OrderService;
import com.cambiz.market.service.TransactionService;

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

    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStats() {
        List<User> allUsers = userRepository.findAll();
        List<Transaction> allTxns = transactionService.getAllTransactions();
        
        double totalRevenue = allTxns.stream()
                .filter(t -> t.getType() == TransactionType.PURCHASE)
                .mapToDouble(Transaction::getPlatformFee)
                .sum();

        AdminStatsDTO stats = AdminStatsDTO.builder()
                .totalUsers(allUsers.size())
                .totalSellers(allUsers.stream().filter(u -> u.getUserType() == User.UserType.SELLER).count())
                .totalBuyers(allUsers.stream().filter(u -> u.getUserType() == User.UserType.BUYER).count())
                .totalOrders(orderService.getAllOrders().size())
                .totalProducts(productRepository.count())
                .premiumSellers(allUsers.stream().filter(u -> "PREMIUM".equals(u.getAccountType())).count())
                .totalRevenue(totalRevenue)
                .pendingDisputes(0)
                .build();

        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }

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

    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() {
        var orders = orderService.getAllOrders();
        return ResponseEntity.ok(Map.of("success", true, "data", orders, "count", orders.size()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions() {
        var transactions = transactionService.getAllTransactions();
        transactions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return ResponseEntity.ok(Map.of("success", true, "data", transactions, "count", transactions.size()));
    }

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

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long userId, @RequestParam String status) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        user.setStatus(User.UserStatus.valueOf(status));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "User status updated to " + status));
    }

    @PutMapping("/sellers/{sellerId}/verify")
    public ResponseEntity<?> verifySeller(@PathVariable Long sellerId) {
        User seller = userRepository.findById(sellerId).orElse(null);
        if (seller == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Seller not found"));
        seller.setAccountType("PREMIUM");
        userRepository.save(seller);
        return ResponseEntity.ok(Map.of("success", true, "message", "Seller verified as Premium"));
    }

    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("apiStatus", "RUNNING");
        health.put("databaseStatus", "CONNECTED");
        health.put("uptime", "24/7");
        health.put("memoryUsage", Runtime.getRuntime().totalMemory() / (1024 * 1024) + " MB");
        health.put("freeMemory", Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
        return ResponseEntity.ok(Map.of("success", true, "data", health));
    }
}