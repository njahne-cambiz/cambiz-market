package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.UserRepository;
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

    @PostMapping("/upgrade")
    public ResponseEntity<ApiResponse> upgradeToPremium(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String plan = request.get("plan").toString(); // monthly, quarterly, yearly
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found", null));
        }
        
        double price;
        LocalDateTime premiumUntil;
        switch (plan) {
            case "monthly": price = 10000; premiumUntil = LocalDateTime.now().plusMonths(1); break;
            case "quarterly": price = 27000; premiumUntil = LocalDateTime.now().plusMonths(3); break;
            case "yearly": price = 96000; premiumUntil = LocalDateTime.now().plusYears(1); break;
            default: return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid plan", null));
        }
        
        user.setAccountType("PREMIUM");
        user.setPremiumUntil(premiumUntil);
        user.setCommissionRate(4.5); // Lower commission for premium
        userRepository.save(user);
        
        return ResponseEntity.ok(new ApiResponse(true, 
            "Upgraded to Premium! Dial *126*1*1*" + (int)price + "# to pay",
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
