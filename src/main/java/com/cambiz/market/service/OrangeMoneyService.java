package com.cambiz.market.service;

import com.cambiz.market.dto.PaymentRequest;
import com.cambiz.market.dto.PaymentResponse;
import com.cambiz.market.dto.PaymentStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class OrangeMoneyService implements PaymentProvider {
    
    @Value("${orange.api.base-url:https://api.orange.com/orange-money-webpay}")
    private String baseUrl;
    
    @Value("${orange.api.client-id:test-client}")
    private String clientId;
    
    @Value("${orange.api.client-secret:test-secret}")
    private String clientSecret;
    
    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating Orange Money payment for order: {}", request.getOrderId());
        
        // Validate phone number format for Orange (starts with 69, 65, etc.)
        if (request.getPhoneNumber() != null && !isValidOrangeNumber(request.getPhoneNumber())) {
            log.warn("Phone number may not be Orange: {}", request.getPhoneNumber());
        }
        
        String transactionId = "OM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        log.info("Orange Money transaction created: {} for amount: {} XAF", transactionId, request.getAmount());
        
        return PaymentResponse.builder()
            .transactionId(transactionId)
            .orderId(request.getOrderId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .paymentMethod("ORANGE_MONEY")
            .status("PENDING")
            .phoneNumber(request.getPhoneNumber())
            .ussdCode("#150*1#")
            .message("Please dial #150*1# and select 'Pay Merchant'. Enter code: " + transactionId)
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    @Override
    public PaymentStatusResponse checkPaymentStatus(String transactionId) {
        log.info("Checking Orange Money payment status: {}", transactionId);
        
        // In production, call Orange Money API
        // GET {baseUrl}/transaction/{transactionId}
        
        return PaymentStatusResponse.builder()
            .transactionId(transactionId)
            .status("SUCCESS")
            .message("Payment completed successfully via Orange Money")
            .completedAt(LocalDateTime.now())
            .lastChecked(LocalDateTime.now())
            .build();
    }
    
    @Override
    public PaymentResponse processCallback(String callbackData) {
        log.info("Processing Orange Money callback: {}", callbackData);
        
        // In production, parse XML/JSON callback from Orange
        // Extract transactionId and status
        
        return PaymentResponse.builder()
            .status("SUCCESS")
            .message("Orange Money callback processed successfully")
            .build();
    }
    
    @Override
    public boolean supports(String paymentMethod) {
        return "ORANGE_MONEY".equalsIgnoreCase(paymentMethod);
    }
    
    /**
     * Validate if phone number is Orange Cameroon format
     * Orange numbers start with: 69, 65, 67, 68
     */
    private boolean isValidOrangeNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        String clean = phoneNumber.replaceAll("[^0-9]", "");
        return clean.startsWith("23769") || clean.startsWith("69") ||
               clean.startsWith("23765") || clean.startsWith("65") ||
               clean.startsWith("23767") || clean.startsWith("67") ||
               clean.startsWith("23768") || clean.startsWith("68");
    }
}