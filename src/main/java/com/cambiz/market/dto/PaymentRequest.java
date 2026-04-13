package com.cambiz.market.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Payment method is required")
    private String paymentMethod; // MTN_MONEY, ORANGE_MONEY, CARD
    
    @NotNull(message = "Phone number is required for mobile money")
    private String phoneNumber;
    
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private String currency = "XAF";
    private String returnUrl; // For card payments
}