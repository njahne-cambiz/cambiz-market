package com.cambiz.market.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/store-tracking")
public class StoreTrackingController {
    
    // Static in-memory tracking shared across all instances
    private static final Map<Long, List<Map<String, Object>>> trackingStore = new LinkedHashMap<>();
    
    public static void addTrackingEntry(Long orderId, String status, String note) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("status", status);
        event.put("displayName", status);
        event.put("description", note);
        event.put("note", note);
        event.put("timestamp", new Date().toString());
        
        trackingStore.computeIfAbsent(orderId, k -> new ArrayList<>()).add(0, event);
    }
    
    @PutMapping("/{orderId}")
    public Map<String, Object> updateStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "") String note) {
        
        addTrackingEntry(orderId, status, note.isEmpty() ? "Status updated to " + status : note);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Status updated to " + status);
        response.put("data", trackingStore.get(orderId).get(0));
        return response;
    }
    
    @GetMapping("/{orderId}")
    public Map<String, Object> getTracking(@PathVariable Long orderId) {
        List<Map<String, Object>> history = trackingStore.getOrDefault(orderId, new ArrayList<>());
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", history);
        response.put("count", history.size());
        return response;
    }
}