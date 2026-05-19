package com.cambiz.market.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dispute {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;
    
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;
    
    @Column(nullable = false)
    private Double amount;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;
    
    @Column(columnDefinition = "TEXT")
    private String evidence;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DisputeStatus status = DisputeStatus.OPEN;
    
    @Column(columnDefinition = "TEXT")
    private String resolution;
    
    @Column(name = "resolved_by")
    private String resolvedBy;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    public enum DisputeStatus {
        OPEN, UNDER_REVIEW, RESOLVED_RELEASED, RESOLVED_REFUNDED, CLOSED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}