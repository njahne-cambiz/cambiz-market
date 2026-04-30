package com.cambiz.market.controller;

import com.cambiz.market.model.OrderStatus;
import com.cambiz.market.model.TrackingEvent;
import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.OrderService;
import com.cambiz.market.service.OrderTrackingService;
import com.cambiz.market.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tracking")
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
            response.put("message", "Invalid status. Valid values: " + java.util.Arrays.toString(OrderStatus.values()));
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderTracking(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId) {
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
    
    @GetMapping("/{orderId}/latest")
    public ResponseEntity<?> getOrderStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId) {
        try {
            TrackingEvent latest = trackingService.getLatestStatus(orderId);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            
            if (latest == null) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("status", "PENDING");
                data.put("displayName", "Pending");
                response.put("data", data);
            } else {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("status", latest.getStatus().name());
                data.put("displayName", latest.getStatus().getDisplayName());
                data.put("description", latest.getStatus().getDescription());
                data.put("timestamp", latest.getTimestamp().toString());
                response.put("data", data);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}