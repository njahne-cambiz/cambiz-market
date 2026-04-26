package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.service.ExpiryScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private ExpiryScheduler expiryScheduler;

    @PostMapping("/expire-featured")
    public ResponseEntity<ApiResponse> expireFeatured() {
        int count = expiryScheduler.manualExpireFeatured();
        return ResponseEntity.ok(new ApiResponse(true, "Expired " + count + " featured listings", Map.of("expiredCount", count)));
    }

    @PostMapping("/expire-premium")
    public ResponseEntity<ApiResponse> expirePremium() {
        int count = expiryScheduler.manualExpirePremium();
        return ResponseEntity.ok(new ApiResponse(true, "Expired " + count + " premium subscriptions", Map.of("expiredCount", count)));
    }
}