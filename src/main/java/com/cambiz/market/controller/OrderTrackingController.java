package com.cambiz.market.controller;

import com.cambiz.market.model.OrderStatus;
import com.cambiz.market.model.TrackingEvent;
import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.OrderService;
import com.cambiz.market.service.OrderTrackingService;
import com.cambiz.market.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderTrackingController {
    
    private final OrderTrackingService trackingService;
    private final OrderService orderService;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    
    public OrderTrackingController(OrderTrackingService trackingService,
                                  OrderService orderService,
                                  JwtUtils jwtUtils,
                                  UserService userService) {
        this.trackingService = trackingService;
        this.orderService = orderService;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }
    
    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtils.extractUsername(token);
        return userService.getUserIdByEmail(email);
    }
    
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "") String note) {
        try {
            Long userId = getUserIdFromToken(authHeader);
            OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
            
            // Update order status in OrderService
            orderService.updateOrderStatus(orderId, newStatus.name());
            
            // Add tracking event
            TrackingEvent event = trackingService.addTrackingEvent(orderId, newStatus, note, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order status updated to " + newStatus.getDisplayName());
            response.put("data", Map.of(
                "status", event.getStatus().name(),
                "displayName", event.getStatus().getDisplayName(),
                "description", event.getStatus().getDescription(),
                "note", event.getNote(),
                "timestamp", event.getTimestamp().toString()
            ));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid status. Valid values: " + java.util.Arrays.toString(OrderStatus.values()));
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<?> getOrderTracking(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId) {
        try {
            List<TrackingEvent> history = trackingService.getTrackingHistory(orderId);
            
            List<Map<String, Object>> timeline = history.stream().map(t -> {
                Map<String, Object> event = new HashMap<>();
                event.put("status", t.getStatus().name());
                event.put("displayName", t.getStatus().getDisplayName());
                event.put("description", t.getStatus().getDescription());
                event.put("note", t.getNote() != null ? t.getNote() : "");
                event.put("timestamp", t.getTimestamp().toString());
                return event;
            }).toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", timeline);
            response.put("count", timeline.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{orderId}/status")
    public ResponseEntity<?> getOrderStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId) {
        try {
            TrackingEvent latest = trackingService.getLatestStatus(orderId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            if (latest == null) {
                response.put("data", Map.of("status", "PENDING", "displayName", "Pending"));
            } else {
                response.put("data", Map.of(
                    "status", latest.getStatus().name(),
                    "displayName", latest.getStatus().getDisplayName(),
                    "description", latest.getStatus().getDescription(),
                    "timestamp", latest.getTimestamp().toString()
                ));
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}