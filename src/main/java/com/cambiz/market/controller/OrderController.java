package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.dto.CheckoutRequest;
import com.cambiz.market.dto.OrderResponse;
import com.cambiz.market.service.OrderService;
import com.cambiz.market.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderService orderService;
    private final UserService userService;
    
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse> checkout(@RequestBody CheckoutRequest request) {
        try {
            Long userId = getCurrentUserId();
            OrderResponse order = orderService.checkout(userId, request);
            return ResponseEntity.ok(new ApiResponse(true, "Order placed successfully!", order));
        } catch (RuntimeException e) {
            log.error("Checkout error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse> getMyOrders() {
        try {
            Long userId = getCurrentUserId();
            var orders = orderService.getBuyerOrders(userId);
            return ResponseEntity.ok(new ApiResponse(true, "Orders retrieved successfully", orders));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse> getOrder(@PathVariable Long orderId) {
        try {
            Long userId = getCurrentUserId();
            OrderResponse order = orderService.getOrder(orderId, userId);
            return ResponseEntity.ok(new ApiResponse(true, "Order retrieved successfully", order));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userService.getUserIdByEmail(email);
        }
        throw new RuntimeException("User not authenticated");
    }
}