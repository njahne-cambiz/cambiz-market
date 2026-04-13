package com.cambiz.market.dto;

import com.cambiz.market.model.Payment;
import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long paymentId;
    private String transactionId;
    private Long orderId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String phoneNumber;
    private String ussdCode;        // For mobile money
    private String paymentUrl;      // For card payments
    private String message;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    
    public static PaymentResponse fromPayment(Payment payment) {
        return PaymentResponse.builder()
            .paymentId(payment.getId())
            .transactionId(payment.getTransactionId())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .paymentMethod(payment.getPaymentMethod().name())
            .status(payment.getStatus().name())
            .phoneNumber(payment.getPhoneNumber())
            .createdAt(payment.getCreatedAt())
            .build();
    }
}