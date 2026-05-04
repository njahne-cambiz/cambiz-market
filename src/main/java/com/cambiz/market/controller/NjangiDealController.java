package com.cambiz.market.controller;

import com.cambiz.market.model.NjangiDeal;
import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.NjangiDealService;
import com.cambiz.market.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/njangi")
@CrossOrigin(origins = "*")
public class NjangiDealController {
    
    private final NjangiDealService njangiService;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    
    public NjangiDealController(NjangiDealService njangiService, JwtUtils jwtUtils, UserService userService) {
        this.njangiService = njangiService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }
    
    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtils.extractUsername(token);
        return userService.getUserIdByEmail(email);
    }
    
    @PostMapping("/create")
    public ResponseEntity<?> createDeal(@RequestHeader("Authorization") String authHeader,
                                         @RequestBody Map<String, Object> request) {
        try {
            Long sellerId = getUserIdFromToken(authHeader);
            var user = userService.findById(sellerId);
            String sellerName = user != null ? (user.getFirstName() + " " + user.getLastName()) : "Seller";
            if (user.getBusinessName() != null && !user.getBusinessName().isEmpty()) {
                sellerName = user.getBusinessName();
            }
            
            Long productId = Long.valueOf(request.get("productId").toString());
            String productName = request.get("productName").toString();
            int minParticipants = Integer.parseInt(request.get("minParticipants").toString());
            int maxParticipants = Integer.parseInt(request.get("maxParticipants").toString());
            double individualPrice = Double.parseDouble(request.get("individualPrice").toString());
            double regularPrice = Double.parseDouble(request.get("regularPrice").toString());
            int durationDays = request.get("durationDays") != null ? Integer.parseInt(request.get("durationDays").toString()) : 7;
            
            NjangiDeal deal = njangiService.createDeal(productId, productName, sellerId, sellerName,
                    minParticipants, maxParticipants, individualPrice, regularPrice, durationDays);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Njangi deal created! Expires in " + durationDays + " days.", "data", deal));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<?> getActiveDeals() {
        List<NjangiDeal> deals = njangiService.getActiveDeals();
        return ResponseEntity.ok(Map.of("success", true, "data", deals, "count", deals.size()));
    }
    
    @GetMapping("/{dealId}")
    public ResponseEntity<?> getDeal(@PathVariable Long dealId) {
        NjangiDeal deal = njangiService.getDeal(dealId);
        if (deal == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Deal not found"));
        return ResponseEntity.ok(Map.of("success", true, "data", deal));
    }
    
    @PostMapping("/{dealId}/join")
    public ResponseEntity<?> joinDeal(@RequestHeader("Authorization") String authHeader,
                                       @PathVariable Long dealId) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            var user = userService.findById(userId);
            String userName = user != null ? (user.getFirstName() + " " + user.getLastName()) : "User";
            String userPhone = user != null ? user.getPhone() : "";
            
            NjangiDeal deal = njangiService.joinDeal(dealId, userId, userName, userPhone);
            if (deal == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Deal not found or not active"));
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Joined Njangi deal!",
                    "data", Map.of("participants", deal.getParticipantCount(), "remaining", deal.getRemainingSpots(), "filled", deal.isFilled())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/my-deals")
    public ResponseEntity<?> getMyDeals(@RequestHeader("Authorization") String authHeader) {
        Long sellerId = getUserIdFromToken(authHeader);
        List<NjangiDeal> deals = njangiService.getSellerDeals(sellerId);
        return ResponseEntity.ok(Map.of("success", true, "data", deals, "count", deals.size()));
    }
}