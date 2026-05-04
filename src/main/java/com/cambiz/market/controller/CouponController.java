package com.cambiz.market.controller;

import com.cambiz.market.model.Coupon;
import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.CouponService;
import com.cambiz.market.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/coupons")
@CrossOrigin(origins = "*")
public class CouponController {
    
    private final CouponService couponService;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    
    public CouponController(CouponService couponService, JwtUtils jwtUtils, UserService userService) {
        this.couponService = couponService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }
    
    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtils.extractUsername(token);
        return userService.getUserIdByEmail(email);
    }
    
    @PostMapping("/create")
    public ResponseEntity<?> createCoupon(@RequestHeader("Authorization") String authHeader,
                                           @RequestBody Map<String, Object> request) {
        try {
            Long sellerId = getUserIdFromToken(authHeader);
            var user = userService.findById(sellerId);
            String sellerName = user.getBusinessName() != null ? user.getBusinessName() : user.getFirstName();
            
            String code = request.get("code").toString();
            double discountPercent = Double.parseDouble(request.get("discountPercent").toString());
            double maxDiscount = Double.parseDouble(request.get("maxDiscount").toString());
            double minOrderAmount = request.get("minOrderAmount") != null ? Double.parseDouble(request.get("minOrderAmount").toString()) : 0;
            int maxUses = Integer.parseInt(request.get("maxUses").toString());
            int expiryDays = Integer.parseInt(request.get("expiryDays").toString());
            boolean isWelcome = request.get("isWelcome") != null && Boolean.parseBoolean(request.get("isWelcome").toString());
            
            Long productId = null;
            String productName = null;
            if (request.get("productId") != null) {
                productId = Long.valueOf(request.get("productId").toString());
                productName = request.get("productName") != null ? request.get("productName").toString() : null;
            }
            
            Coupon coupon = couponService.createCoupon(code, discountPercent, maxDiscount, minOrderAmount,
                    productId, productName, maxUses, sellerId, sellerName, expiryDays, isWelcome);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Coupon created: " + code, "data", coupon));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    @GetMapping("/validate/{code}")
    public ResponseEntity<?> validateCoupon(@PathVariable String code) {
        Coupon coupon = couponService.getCoupon(code);
        if (coupon == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Invalid coupon code"));
        }
        if (!coupon.isValid()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Coupon has expired or been used up"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Valid coupon!", "data", coupon));
    }
    
    @GetMapping("/my-welcome")
    public ResponseEntity<?> getMyWelcomeCoupon(@RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        String code = couponService.generateWelcomeCoupon(userId, "User");
        Coupon coupon = couponService.getCoupon(code);
        return ResponseEntity.ok(Map.of("success", true, "message", "Welcome coupon: " + code, "data", coupon));
    }
    
    @GetMapping("/my-coupons")
    public ResponseEntity<?> getMyCoupons(@RequestHeader("Authorization") String authHeader) {
        Long sellerId = getUserIdFromToken(authHeader);
        List<Coupon> coupons = couponService.getSellerCoupons(sellerId);
        return ResponseEntity.ok(Map.of("success", true, "data", coupons, "count", coupons.size()));
    }
}