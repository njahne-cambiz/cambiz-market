package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.dto.CartResponse;
import com.cambiz.market.service.CartService;
import com.cambiz.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private UserService userService;
    
    // Add item to cart
    @PostMapping("/add/{productId}")
    public ResponseEntity<ApiResponse> addToCart(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            Long userId = getCurrentUserId();
            CartResponse cart = ((CartService) cartService).addToCart(userId, productId, quantity);
            return ResponseEntity.ok(new ApiResponse(true, "Item added to cart", cart));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    // Remove item from cart
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<ApiResponse> removeFromCart(@PathVariable Long productId) {
        try {
            Long userId = getCurrentUserId();
            CartResponse cart = cartService.removeFromCart(userId, productId);
            return ResponseEntity.ok(new ApiResponse(true, "Item removed from cart", cart));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    // Update item quantity
    @PutMapping("/update/{productId}")
    public ResponseEntity<ApiResponse> updateQuantity(
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        try {
            Long userId = getCurrentUserId();
            CartResponse cart = cartService.updateQuantity(userId, productId, quantity);
            return ResponseEntity.ok(new ApiResponse(true, "Cart updated", cart));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    // Get my cart
    @GetMapping
    public ResponseEntity<ApiResponse> getCart() {
        try {
            Long userId = getCurrentUserId();
            CartResponse cart = cartService.getCart(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Cart retrieved", cart));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    // Clear cart
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse> clearCart() {
        try {
            Long userId = getCurrentUserId();
            cartService.clearCart(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Cart cleared", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    // Get cart summary
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse> getCartSummary() {
        try {
            Long userId = getCurrentUserId();
            CartResponse cart = cartService.getCart(userId);
            
            Map<String, Object> summary = new HashMap<>();
            if (cart != null) {
                summary.put("items", cart.getItems() != null ? cart.getItems().size() : 0);
                summary.put("cart", cart);
            } else {
                summary.put("items", 0);
            }
            
            return ResponseEntity.ok(new ApiResponse(true, "Summary retrieved", summary));
        } catch (Exception e) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("items", 0);
            return ResponseEntity.ok(new ApiResponse(true, "Summary retrieved", empty));
        }
    }
    
    // Helper method - ADAPT THIS TO YOUR UserService
    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            
            // REPLACE THIS LINE with whatever method exists in your UserService
            // For example:
            return userService.getUserIdByEmail(email);
            // OR:
            // return userService.findByEmail(email).getId();
            // OR:
            // return userService.getUserByEmail(email).getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}