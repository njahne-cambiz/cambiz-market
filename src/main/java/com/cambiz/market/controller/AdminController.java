package com.cambiz.market.controller;

import com.cambiz.market.dto.AdminStatusDTO;
import com.cambiz.market.model.Transaction;
import com.cambiz.market.model.TransactionType;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.UserRepository;
import com.cambiz.market.service.OrderService;
import com.cambiz.market.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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

        AdminStatusDTO stats = AdminStatusDTO.builder()
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

    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() {
        var orders = orderService.getAllOrders();
        return ResponseEntity.ok(Map.of("success", true, "data", orders, "count", orders.size()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions() {
        var transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(Map.of("success", true, "data", transactions));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long userId, @RequestParam String status) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "User not found"));
        user.setStatus(User.UserStatus.valueOf(status));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "User status updated to " + status));
    }
}