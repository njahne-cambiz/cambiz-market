package com.cambiz.market.controller;

import com.cambiz.market.model.Transaction;
import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.TransactionService;
import com.cambiz.market.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {
    
    private final TransactionService transactionService;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    
    public TransactionController(TransactionService transactionService, JwtUtils jwtUtils, UserService userService) {
        this.transactionService = transactionService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }
    
    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtils.extractUsername(token);
        return userService.getUserIdByEmail(email);
    }
    
    @GetMapping("/my-transactions")
    public ResponseEntity<?> getMyTransactions(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        var user = userService.findById(userId);
        
        List<Transaction> transactions;
        if (user.getUserType() == com.cambiz.market.model.User.UserType.SELLER) {
            transactions = transactionService.getSellerTransactions(userId);
        } else {
            transactions = transactionService.getBuyerTransactions(userId);
        }
        
        return ResponseEntity.ok(Map.of("success", true, "data", transactions, "count", transactions.size()));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        var stats = transactionService.getSellerStats(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }
}