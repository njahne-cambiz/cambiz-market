package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.service.FeaturedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/featured")
@CrossOrigin(origins = "*")
public class FeaturedController {

    @Autowired
    private FeaturedService featuredService;

    @PostMapping("/boost")
    public ResponseEntity<ApiResponse> boostProduct(@RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            Long sellerId = Long.valueOf(request.get("sellerId").toString());
            String duration = request.get("duration").toString();
            
            Map<String, Object> result = featuredService.boostProduct(productId, sellerId, duration);
            
            return ResponseEntity.ok(new ApiResponse(true, 
                "Product boosted! Dial " + result.get("ussdCode") + " to pay", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getFeaturedProducts() {
        try {
            List<Map<String, Object>> featured = featuredService.getFeaturedProducts();
            return ResponseEntity.ok(new ApiResponse(true, "Featured products retrieved", featured));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse(true, "Featured products retrieved", List.of()));
        }
    }
}