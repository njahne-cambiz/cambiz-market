package com.cambiz.market.dto;

import com.cambiz.market.model.PriceNegotiation;
import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MakolaResponse {
    private Long negotiationId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal originalPrice;
    private BigDecimal buyerOffer;
    private BigDecimal sellerCounter;
    private BigDecimal finalPrice;
    private BigDecimal aiSuggestedPrice;
    private String aiMessage;
    private String status;
    private String buyerMessage;
    private String sellerMessage;
    private BigDecimal savings;
    private Integer savingsPercentage;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Boolean canCounter;
    private Boolean canAccept;
    private Boolean canAddToCart;
    
    public static MakolaResponse fromNegotiation(PriceNegotiation negotiation) {
        boolean isActive = negotiation.isActive();
        
        return MakolaResponse.builder()
            .negotiationId(negotiation.getId())
            .productId(negotiation.getProduct().getId())
            .productName(negotiation.getProduct().getName())
            .productImage(negotiation.getProduct().getImageUrl())
            .originalPrice(negotiation.getOriginalPrice())
            .buyerOffer(negotiation.getBuyerOffer())
            .sellerCounter(negotiation.getSellerCounter())
            .finalPrice(negotiation.getFinalPrice())
            .aiSuggestedPrice(negotiation.getAiSuggestedPrice())
            .aiMessage(negotiation.getAiReasoning())
            .status(negotiation.getStatus().name())
            .buyerMessage(negotiation.getBuyerMessage())
            .sellerMessage(negotiation.getSellerMessage())
            .savings(negotiation.getSavings())
            .savingsPercentage(negotiation.getSavingsPercentage())
            .expiresAt(negotiation.getExpiresAt())
            .createdAt(negotiation.getCreatedAt())
            .canCounter(isActive)
            .canAccept(isActive)
            .canAddToCart(negotiation.getStatus() == PriceNegotiation.NegotiationStatus.ACCEPTED)
            .build();
    }
}