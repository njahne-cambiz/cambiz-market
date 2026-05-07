package com.cambiz.market.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "persisted_orders")
public class PersistedOrder {

    @Id
    private Long orderId;

    @Column(unique = true)
    private String orderNumber;

    @Column(columnDefinition = "TEXT")
    private String orderData; // JSON string of OrderResponse

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public PersistedOrder() {}

    public PersistedOrder(Long orderId, String orderNumber, String orderData, String status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderData = orderData;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getOrderData() { return orderData; }
    public void setOrderData(String orderData) { this.orderData = orderData; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}