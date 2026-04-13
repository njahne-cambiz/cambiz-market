package com.cambiz.market.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class MakolaOfferRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Offer amount is required")
    @Positive(message = "Offer must be greater than 0")
    private BigDecimal offerAmount;
    
    private String message; // Optional: "I'm buying in bulk", "Cash payment", etc.
}
