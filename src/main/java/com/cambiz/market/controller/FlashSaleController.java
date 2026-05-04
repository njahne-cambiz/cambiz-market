package com.cambiz.market.controller;

import com.cambiz.market.model.FlashSale;
import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.FlashSaleService;
import com.cambiz.market.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/flash-sales")
@CrossOrigin(origins = "*")
public class FlashSaleController {
    
    private final FlashSaleService flashSaleService;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    
    public FlashSaleController(FlashSaleService flashSaleService, JwtUtils jwtUtils, UserService userService) {
        this.flashSaleService = flashSaleService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }
    
    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtils.extractUsername(token);
        return userService.getUserIdByEmail(email);
    }
    
    @PostMapping("/create")
    public ResponseEntity<?> createFlashSale(@RequestHeader("Authorization") String authHeader,
                                              @RequestBody Map<String, Object> request) {
        try {
            Long sellerId = getUserIdFromToken(authHeader);
            var user = userService.findById(sellerId);
            String sellerName = user.getBusinessName() != null ? user.getBusinessName() : user.getFirstName() + " " + user.getLastName();
            
            Long productId = Long.valueOf(request.get("productId").toString());
            String productName = request.get("productName").toString();
            double originalPrice = Double.parseDouble(request.get("originalPrice").toString());
            double flashPrice = Double.parseDouble(request.get("flashPrice").toString());
            int totalStock = Integer.parseInt(request.get("totalStock").toString());
            int durationHours = Integer.parseInt(request.get("durationHours").toString());
            
            FlashSale sale = flashSaleService.createSale(productId, productName, sellerId, sellerName,
                    originalPrice, flashPrice, totalStock, durationHours);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Flash sale created!", "data", sale));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<?> getActiveSales() {
        List<FlashSale> sales = flashSaleService.getActiveSales();
        return ResponseEntity.ok(Map.of("success", true, "data", sales, "count", sales.size()));
    }
}