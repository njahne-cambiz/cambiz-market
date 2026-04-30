package com.cambiz.market.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.cambiz.market.model.OrderStatus;
import com.cambiz.market.model.TrackingEvent;

@Service
public class OrderTrackingService {
    
    // In-memory tracking storage: orderId -> list of tracking events
    private final ConcurrentHashMap<Long, List<TrackingEvent>> orderTracking = new ConcurrentHashMap<>();
    
    public TrackingEvent addTrackingEvent(Long orderId, OrderStatus status, String note, Long updatedBy) {
        TrackingEvent event = new TrackingEvent(status, note, updatedBy);
        orderTracking.computeIfAbsent(orderId, k -> new ArrayList<>()).add(0, event);
        return event;
    }
    
    public List<TrackingEvent> getTrackingHistory(Long orderId) {
        return orderTracking.getOrDefault(orderId, new ArrayList<>());
    }
    
    public TrackingEvent getLatestStatus(Long orderId) {
        List<TrackingEvent> history = getTrackingHistory(orderId);
        return history.isEmpty() ? null : history.get(0);
    }
}