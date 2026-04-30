package com.cambiz.market.model;

public enum OrderStatus {
    PENDING("Pending", "Order placed, awaiting confirmation"),
    CONFIRMED("Confirmed", "Seller confirmed your order"),
    PROCESSING("Processing", "Order is being prepared"),
    SHIPPED("Shipped", "Order is on the way"),
    OUT_FOR_DELIVERY("Out for Delivery", "Delivery in progress"),
    DELIVERED("Delivered", "Order delivered successfully"),
    CANCELLED("Cancelled", "Order has been cancelled"),
    RETURNED("Returned", "Order has been returned");
    
    private final String displayName;
    private final String description;
    
    OrderStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}