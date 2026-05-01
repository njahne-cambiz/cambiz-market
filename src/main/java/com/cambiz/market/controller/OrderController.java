package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.dto.CheckoutRequest;
import com.cambiz.market.dto.OrderResponse;
import com.cambiz.market.model.OrderStatus;
import com.cambiz.market.model.TrackingEvent;
import com.cambiz.market.service.OrderService;
import com.cambiz.market.service.OrderTrackingService;
import com.cambiz.market.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderService orderService;
    private final UserService userService;
    private final OrderTrackingService trackingService;
    
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
    
    // ========== ORDER TRACKING ==========
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "") String note) {
        try {
            Long userId = getCurrentUserId();
            OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
            
            orderService.updateOrderStatus(orderId, newStatus.name());
            TrackingEvent event = trackingService.addTrackingEvent(orderId, newStatus, note, userId);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Order status updated to " + newStatus.getDisplayName());
            
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("status", event.getStatus().name());
            data.put("displayName", event.getStatus().getDisplayName());
            data.put("description", event.getStatus().getDescription());
            data.put("note", event.getNote());
            data.put("timestamp", event.getTimestamp().toString());
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", "Invalid status. Values: " + Arrays.toString(OrderStatus.values()));
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<?> getOrderTracking(@PathVariable Long orderId) {
        try {
            List<TrackingEvent> history = trackingService.getTrackingHistory(orderId);
            
            List<Map<String, Object>> timeline = new ArrayList<>();
            for (TrackingEvent t : history) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("status", t.getStatus().name());
                event.put("displayName", t.getStatus().getDisplayName());
                event.put("description", t.getStatus().getDescription());
                event.put("note", t.getNote() != null ? t.getNote() : "");
                event.put("timestamp", t.getTimestamp().toString());
                timeline.add(event);
            }
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("data", timeline);
            response.put("count", timeline.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
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