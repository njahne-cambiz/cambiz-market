package com.cambiz.market.controller;

import com.cambiz.market.model.Transaction;
import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.TransactionService;
import com.cambiz.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtils.extractUsername(token);
        return userService.getUserIdByEmail(email);
    }

    @GetMapping("/my-transactions")
    public ResponseEntity<?> getMyTransactions(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserIdFromToken(authHeader);
        var user = userService.findById(userId);

        Page<Transaction> transactions;
        if (user.getUserType() == com.cambiz.market.model.User.UserType.SELLER) {
            transactions = transactionService.getSellerTransactions(userId, page, size);
        } else {
            transactions = transactionService.getBuyerTransactions(userId, page, size);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", transactions.getContent());
        response.put("totalPages", transactions.getTotalPages());
        response.put("totalElements", transactions.getTotalElements());
        response.put("page", page);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        var stats = transactionService.getSellerStats(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }
}