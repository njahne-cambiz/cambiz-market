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
public class MTNMoneyService implements PaymentProvider {
    
    @Value("${mtn.api.base-url:https://sandbox.momodeveloper.mtn.com}")
    private String baseUrl;
    
    @Value("${mtn.api.primary-key:test-key}")
    private String primaryKey;
    
    @Value("${mtn.api.secondary-key:test-secondary-key}")
    private String secondaryKey;
    
    @Value("${mtn.api.target-environment:sandbox}")
    private String targetEnvironment;
    
    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("========================================");
        log.info("Initiating MTN Money payment");
        log.info("Order ID: {}", request.getOrderId());
        log.info("Amount: {} {}", request.getAmount(), request.getCurrency());
        log.info("Phone: {}", request.getPhoneNumber());
        log.info("========================================");
        
        // Validate phone number format for MTN Cameroon
        if (request.getPhoneNumber() != null && !isValidMTNNumber(request.getPhoneNumber())) {
            log.warn("Phone number may not be MTN Cameroon: {}", request.getPhoneNumber());
        }
        
        // Generate unique transaction ID
        String transactionId = "MTN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // In production, this would call MTN Momo API
        // Step 1: Create API User
        // Step 2: Get API Key
        // Step 3: Create Access Token
        // Step 4: Request to Pay
        
        log.info("MTN Money transaction created: {} for amount: {} XAF", transactionId, request.getAmount());
        log.info("USSD Code: *126*1*1# or dial {}", formatUSSD(request.getAmount()));
        
        return PaymentResponse.builder()
            .transactionId(transactionId)
            .orderId(request.getOrderId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .paymentMethod("MTN_MONEY")
            .status("PENDING")
            .phoneNumber(request.getPhoneNumber())
            .ussdCode("*126*1*1#")
            .message(buildPaymentMessage(request.getAmount(), transactionId))
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    @Override
    public PaymentStatusResponse checkPaymentStatus(String transactionId) {
        log.info("Checking MTN Money payment status: {}", transactionId);
        
        // In production, call MTN API
        // GET {baseUrl}/collection/v1_0/requesttopay/{transactionId}
        
        // Simulate checking status
        // In real implementation, would return PENDING, SUCCESS, or FAILED
        
        log.info("MTN Money payment {} status: SUCCESS", transactionId);
        
        return PaymentStatusResponse.builder()
            .transactionId(transactionId)
            .status("SUCCESS")
            .message("Payment completed successfully via MTN Mobile Money")
            .completedAt(LocalDateTime.now())
            .lastChecked(LocalDateTime.now())
            .build();
    }
    
    @Override
    public PaymentResponse processCallback(String callbackData) {
        log.info("========================================");
        log.info("Processing MTN Money callback");
        log.info("Callback data: {}", callbackData);
        log.info("========================================");
        
        // In production, parse JSON callback from MTN
        // Extract transactionId, status, amount, etc.
        // Update payment record in database
        
        return PaymentResponse.builder()
            .status("SUCCESS")
            .message("MTN Money callback processed successfully")
            .build();
    }
    
    @Override
    public boolean supports(String paymentMethod) {
        return "MTN_MONEY".equalsIgnoreCase(paymentMethod) || 
               "MTN".equalsIgnoreCase(paymentMethod) ||
               "MOMO".equalsIgnoreCase(paymentMethod);
    }
    
    /**
     * Validate if phone number is MTN Cameroon format
     * MTN Cameroon numbers start with: 67, 68, 69
     */
    private boolean isValidMTNNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        
        // Remove all non-digit characters
        String clean = phoneNumber.replaceAll("[^0-9]", "");
        
        // Check for MTN Cameroon prefixes
        // Formats: 23767xxxxxxx, 23768xxxxxxx, 23769xxxxxxx
        // Or without country code: 67xxxxxx, 68xxxxxx, 69xxxxxx
        return clean.startsWith("23767") || clean.startsWith("67") ||
               clean.startsWith("23768") || clean.startsWith("68") ||
               clean.startsWith("23769") || clean.startsWith("69");
    }
    
    /**
     * Format USSD code with amount
     * Example: *126*1*1*650000#
     */
    private String formatUSSD(java.math.BigDecimal amount) {
        return "*126*1*1*" + amount.longValue() + "#";
    }
    
    /**
     * Build user-friendly payment message
     */
    private String buildPaymentMessage(java.math.BigDecimal amount, String transactionId) {
        StringBuilder message = new StringBuilder();
        message.append("📱 MTN Mobile Money Payment\n\n");
        message.append("Option 1: Dial ").append(formatUSSD(amount)).append("\n");
        message.append("Option 2: Dial *126# and select:\n");
        message.append("  1. MoMoPay\n");
        message.append("  2. Pay Merchant\n");
        message.append("  3. Enter amount: ").append(amount.longValue()).append(" XAF\n");
        message.append("  4. Enter PIN to confirm\n\n");
        message.append("Reference: ").append(transactionId).append("\n");
        message.append("Expires in: 15 minutes");
        
        return message.toString();
    }
    
    /**
     * Get payment methods supported by this provider
     */
    public java.util.List<String> getSupportedMethods() {
        return java.util.List.of("MTN_MONEY", "MTN", "MOMO");
    }
    
    /**
     * Check if provider is available/configured
     */
    public boolean isAvailable() {
        return baseUrl != null && !baseUrl.isEmpty() && 
               primaryKey != null && !primaryKey.isEmpty();
    }
    
    /**
     * Get provider display name
     */
    public String getDisplayName() {
        return "MTN Mobile Money";
    }
    
    /**
     * Get provider logo URL
     */
    public String getLogoUrl() {
        return "https://cambiz.cm/images/mtn-money-logo.png";
    }
    
    /**
     * Get provider USSD code
     */
    public String getUssdCode() {
        return "*126#";
    }
    
    /**
     * Get supported currencies
     */
    public java.util.List<String> getSupportedCurrencies() {
        return java.util.List.of("XAF", "XOF", "GHS", "NGN");
    }
    
    /**
     * Get minimum transaction amount
     */
    public java.math.BigDecimal getMinimumAmount() {
        return new java.math.BigDecimal("100");
    }
    
    /**
     * Get maximum transaction amount
     */
    public java.math.BigDecimal getMaximumAmount() {
        return new java.math.BigDecimal("5000000");
    }
}