package com.cambiz.market.model;

import java.time.LocalDateTime;

public class TrackingEvent {
    private OrderStatus status;
    private String note;
    private Long updatedBy;
    private LocalDateTime timestamp;
    
    public TrackingEvent() {}
    
    public TrackingEvent(OrderStatus status, String note, Long updatedBy) {
        this.status = status;
        this.note = note;
        this.updatedBy = updatedBy;
        this.timestamp = LocalDateTime.now();
    }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}