package com.cambiz.market.controller;

import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.ReferralService;
import com.cambiz.market.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/referral")
@CrossOrigin(origins = "*")
public class ReferralController {
    
    private final ReferralService referralService;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    
    public ReferralController(ReferralService referralService, JwtUtils jwtUtils, UserService userService) {
        this.referralService = referralService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }
    
    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtils.extractUsername(token);
        return userService.getUserIdByEmail(email);
    }
    
    @GetMapping("/my-code")
    public ResponseEntity<?> getMyReferralCode(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        String code = referralService.getReferralCode(userId);
        return ResponseEntity.ok(Map.of("success", true, "referralCode", code,
                "referralLink", "https://cambiz-market.onrender.com/register?ref=" + code));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getMyStats(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        var stats = referralService.getReferralStats(userId);
        return ResponseEntity.ok(Map.of("success", true, "data", stats));
    }
}