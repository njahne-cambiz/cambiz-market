package com.cambiz.market.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class MakolaCounterRequest {
    
    @NotNull(message = "Counter offer amount is required")
    @Positive(message = "Counter offer must be greater than 0")
    private BigDecimal counterAmount;
    
    private String message; // Optional: "This is my best price"
}