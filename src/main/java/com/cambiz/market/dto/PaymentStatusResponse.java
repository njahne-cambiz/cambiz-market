package com.cambiz.market.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentStatusResponse {
    private String transactionId;
    private Long orderId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String message;
    private LocalDateTime completedAt;
    private LocalDateTime lastChecked;
}
