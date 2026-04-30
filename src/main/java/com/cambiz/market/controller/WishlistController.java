package com.cambiz.market.controller;

import com.cambiz.market.dto.WishlistResponse;
import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.UserService;
import com.cambiz.market.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
    
    private final WishlistService wishlistService;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    
    public WishlistController(WishlistService wishlistService, 
                             JwtUtils jwtUtils,
                             UserService userService) {
        this.wishlistService = wishlistService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }
    
    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtils.extractUsername(token);
        return userService.getUserIdByEmail(email);
    }
    
    @PostMapping("/add/{productId}")
    public ResponseEntity<?> addToWishlist(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId) {
        Long userId = getUserIdFromToken(authHeader);
        String message = wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.ok(Map.of("success", true, "message", message));
    }
    
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeFromWishlist(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId) {
        Long userId = getUserIdFromToken(authHeader);
        String message = wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(Map.of("success", true, "message", message));
    }
    
    @GetMapping
    public ResponseEntity<?> getWishlist(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        List<WishlistResponse> wishlist = wishlistService.getWishlist(userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", wishlist,
            "count", wishlist.size()
        ));
    }
    
    @GetMapping("/check/{productId}")
    public ResponseEntity<?> checkWishlist(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId) {
        Long userId = getUserIdFromToken(authHeader);
        boolean inWishlist = wishlistService.isInWishlist(userId, productId);
        return ResponseEntity.ok(Map.of("success", true, "inWishlist", inWishlist));
    }
    
    @GetMapping("/count")
    public ResponseEntity<?> getWishlistCount(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromToken(authHeader);
        long count = wishlistService.getWishlistCount(userId);
        return ResponseEntity.ok(Map.of("success", true, "count", count));
    }
}