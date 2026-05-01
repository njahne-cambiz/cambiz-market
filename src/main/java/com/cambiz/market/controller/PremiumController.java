package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.UserRepository;
import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/premium")
@CrossOrigin(origins = "*")
public class PremiumController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserService userService;

    @PostMapping("/upgrade")
    public ResponseEntity<ApiResponse> upgradeToPremium(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        
        // Extract user from JWT
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtils.extractUsername(token);
        Long userId = userService.getUserIdByEmail(email);
        
        String plan = request.get("plan").toString(); // MONTHLY, QUARTERLY, YEARLY
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found", null));
        }
        
        double price;
        LocalDateTime premiumUntil;
        switch (plan.toUpperCase()) {
            case "MONTHLY": price = 10000; premiumUntil = LocalDateTime.now().plusMonths(1); break;
            case "QUARTERLY": price = 27000; premiumUntil = LocalDateTime.now().plusMonths(3); break;
            case "YEARLY": price = 96000; premiumUntil = LocalDateTime.now().plusYears(1); break;
            default: return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid plan: " + plan, null));
        }
        
        user.setAccountType("PREMIUM");
        user.setPremiumUntil(premiumUntil);
        user.setCommissionRate(4.5);
        userRepository.save(user);
        
        return ResponseEntity.ok(new ApiResponse(true, 
            "🎉 You are now a Premium Seller! Plan: " + plan + " | Valid until: " + premiumUntil.toLocalDate(),
            Map.of("userId", userId, "plan", plan, "price", price,
                   "premiumUntil", premiumUntil.toString(),
                   "ussdCode", "*126*1*1*" + (int)price + "#")));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse> checkPremiumStatus(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found", null));
        }
        
        boolean isPremium = "PREMIUM".equals(user.getAccountType()) 
                && user.getPremiumUntil() != null 
                && user.getPremiumUntil().isAfter(LocalDateTime.now());
        
        return ResponseEntity.ok(new ApiResponse(true, "Premium status", Map.of(
            "isPremium", isPremium,
            "accountType", user.getAccountType(),
            "premiumUntil", user.getPremiumUntil() != null ? user.getPremiumUntil().toString() : null,
            "commissionRate", user.getCommissionRate()
        )));
    }

    @GetMapping("/benefits")
    public ResponseEntity<ApiResponse> getBenefits() {
        return ResponseEntity.ok(new ApiResponse(true, "Premium benefits", Map.of(
            "verifiedBadge", true,
            "commissionRate", "4.5%",
            "featuredDiscount", "20% off",
            "prioritySupport", "4h response",
            "pricing", Map.of(
                "monthly", "10,000 XAF",
                "quarterly", "27,000 XAF (save 3,000)",
                "yearly", "96,000 XAF (save 24,000)"
            )
        )));
    }
}