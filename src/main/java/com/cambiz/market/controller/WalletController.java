package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse> getWallet(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found", null));
        }
        return ResponseEntity.ok(new ApiResponse(true, "Wallet retrieved", Map.of(
            "userId", user.getId(),
            "balance", user.getWalletBalance() != null ? user.getWalletBalance() : 0.0,
            "totalEarned", user.getTotalEarned() != null ? user.getTotalEarned() : 0.0,
            "accountType", user.getAccountType(),
            "commissionRate", user.getCommissionRate()
        )));
    }

    @PostMapping("/{userId}/credit")
    public ResponseEntity<ApiResponse> creditWallet(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found", null));
        }
        
        double amount = Double.valueOf(request.get("amount").toString());
        user.setWalletBalance((user.getWalletBalance() != null ? user.getWalletBalance() : 0.0) + amount);
        user.setTotalEarned((user.getTotalEarned() != null ? user.getTotalEarned() : 0.0) + amount);
        userRepository.save(user);
        
        return ResponseEntity.ok(new ApiResponse(true, "Wallet credited with " + amount + " XAF", Map.of(
            "userId", user.getId(),
            "newBalance", user.getWalletBalance(),
            "credited", amount
        )));
    }

    @PostMapping("/{userId}/debit")
    public ResponseEntity<ApiResponse> debitWallet(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found", null));
        }
        
        double amount = Double.valueOf(request.get("amount").toString());
        double balance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        
        if (balance < amount) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Insufficient balance", null));
        }
        
        user.setWalletBalance(balance - amount);
        userRepository.save(user);
        
        return ResponseEntity.ok(new ApiResponse(true, "Wallet debited by " + amount + " XAF", Map.of(
            "userId", user.getId(),
            "newBalance", user.getWalletBalance(),
            "debited", amount
        )));
    }

    @PostMapping("/{userId}/payout")
    public ResponseEntity<ApiResponse> requestPayout(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found", null));
        }
        
        double balance = user.getWalletBalance() != null ? user.getWalletBalance() : 0.0;
        if (balance <= 0) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "No balance to payout", null));
        }
        
        // Reset wallet after payout
        double payoutAmount = balance;
        user.setWalletBalance(0.0);
        userRepository.save(user);
        
        return ResponseEntity.ok(new ApiResponse(true, "Payout of " + payoutAmount + " XAF requested via MTN Money", Map.of(
            "userId", user.getId(),
            "payoutAmount", payoutAmount,
            "phone", user.getPhone(),
            "ussdCode", "*126*1*1*" + (int)payoutAmount + "#"
        )));
    }
}