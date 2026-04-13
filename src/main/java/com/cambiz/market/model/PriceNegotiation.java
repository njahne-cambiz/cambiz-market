package com.cambiz.market.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_negotiations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceNegotiation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;
    
    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;
    
    @Column(name = "buyer_offer", precision = 10, scale = 2)
    private BigDecimal buyerOffer;
    
    @Column(name = "seller_counter", precision = 10, scale = 2)
    private BigDecimal sellerCounter;
    
    @Column(name = "final_price", precision = 10, scale = 2)
    private BigDecimal finalPrice;
    
    @Column(name = "buyer_message", length = 500)
    private String buyerMessage;
    
    @Column(name = "seller_message", length = 500)
    private String sellerMessage;
    
    @Enumerated(EnumType.STRING)
    private NegotiationStatus status = NegotiationStatus.PENDING;
    
    @Column(name = "ai_suggested_price", precision = 10, scale = 2)
    private BigDecimal aiSuggestedPrice;
    
    @Column(name = "ai_reasoning", length = 500)
    private String aiReasoning;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusHours(24); // Offer expires in 24 hours
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum NegotiationStatus {
        PENDING,           // Waiting for seller response
        COUNTERED,         // Seller made counter offer
        ACCEPTED,          // Both agreed
        REJECTED,          // Seller rejected
        EXPIRED,           // Offer expired
        CONVERTED_TO_CART  // Added to cart with negotiated price
    }
    
    // Helper method to check if negotiation is active
    public boolean isActive() {
        return status == NegotiationStatus.PENDING || 
               status == NegotiationStatus.COUNTERED;
    }
    
    // Helper method to calculate savings
    public BigDecimal getSavings() {
        if (finalPrice != null && originalPrice != null) {
            return originalPrice.subtract(finalPrice);
        }
        return BigDecimal.ZERO;
    }
    
    // Helper method to get savings percentage
    @SuppressWarnings("deprecation")
	public int getSavingsPercentage() {
        if (finalPrice != null && originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            return finalPrice.multiply(new BigDecimal("100"))
                .divide(originalPrice, 0, BigDecimal.ROUND_HALF_UP)
                .intValue();
        }
        return 0;
    }
}