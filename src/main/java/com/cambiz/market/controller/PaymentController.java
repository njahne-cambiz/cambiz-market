package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.dto.PaymentRequest;
import com.cambiz.market.dto.PaymentResponse;
import com.cambiz.market.dto.PaymentStatusResponse;
import com.cambiz.market.security.PaymentSignatureVerifier;
import com.cambiz.market.service.AuditService;
import com.cambiz.market.service.PaymentService;
import com.cambiz.market.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private final PaymentService paymentService;
    private final PaymentSignatureVerifier signatureVerifier;
    private final AuditService auditService;
    private final UserService userService;
    
    /**
     * Initiate payment
     * POST /api/payments/initiate
     */
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.initiatePayment(request);
            
            // Audit logging
            Long userId = getCurrentUserId();
            String userEmail = getCurrentUserEmail();
            Map<String, Object> details = new HashMap<>();
            details.put("orderId", request.getOrderId());
            details.put("paymentMethod", request.getPaymentMethod());
            details.put("amount", request.getAmount());
            auditService.log("PAYMENT_INITIATED", userId, userEmail, "PAYMENT", 
                response.getPaymentId() != null ? response.getPaymentId() : 0L, details, "SUCCESS");
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Payment initiated. " + response.getMessage(),
                response
            ));
        } catch (RuntimeException e) {
            log.error("Payment initiation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * Check payment status
     * GET /api/payments/status/{transactionId}
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<ApiResponse> checkStatus(@PathVariable String transactionId) {
        try {
            PaymentStatusResponse response = paymentService.checkPaymentStatus(transactionId);
            
            String message = "SUCCESS".equals(response.getStatus()) 
                ? "Payment completed successfully!" 
                : "Payment status: " + response.getStatus();
            
            // Audit logging for successful payments
            if ("SUCCESS".equals(response.getStatus())) {
                Long userId = getCurrentUserId();
                String userEmail = getCurrentUserEmail();
                Map<String, Object> details = new HashMap<>();
                details.put("transactionId", transactionId);
                auditService.log("PAYMENT_SUCCESS", userId, userEmail, "PAYMENT", 
                    0L, details, "SUCCESS");
            }
            
            return ResponseEntity.ok(new ApiResponse(true, message, response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get payment by order ID
     * GET /api/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse> getPaymentByOrder(@PathVariable Long orderId) {
        try {
            PaymentResponse response = paymentService.getPaymentByOrder(orderId);
            return ResponseEntity.ok(new ApiResponse(true, "Payment retrieved", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * Payment callback from provider - WITH SIGNATURE VERIFICATION
     * POST /api/payments/callback/{provider}
     */
    @PostMapping("/callback/{provider}")
    public ResponseEntity<ApiResponse> handleCallback(
            @PathVariable String provider,
            @RequestBody String callbackData,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-Transaction-Id", required = false) String transactionId) {
        
        try {
            log.info("========================================");
            log.info("Received callback from: {}", provider.toUpperCase());
            log.info("Transaction ID: {}", transactionId);
            log.info("Signature present: {}", signature != null ? "YES" : "NO");
            log.info("========================================");
            
            // Verify signature before processing
            boolean isValid = false;
            String normalizedProvider = provider.toLowerCase();
            
            if ("mtn".equals(normalizedProvider) || "mtn_money".equals(normalizedProvider)) {
                isValid = signatureVerifier.verifyMTNSignature(callbackData, signature);
            } else if ("orange".equals(normalizedProvider) || "orange_money".equals(normalizedProvider)) {
                isValid = signatureVerifier.verifyOrangeSignature(callbackData, signature);
            } else {
                log.warn("Unknown provider: {}", provider);
            }
            
            if (!isValid) {
                log.error("❌ INVALID SIGNATURE - Possible fraud attempt!");
                log.error("Provider: {}, Signature: {}", provider, signature);
                
                // Audit logging for fraud attempt
                Map<String, Object> fraudDetails = new HashMap<>();
                fraudDetails.put("provider", provider);
                fraudDetails.put("reason", "Invalid signature");
                auditService.log("FRAUD_ATTEMPT", null, "system", "PAYMENT", 
                    null, fraudDetails, "FAILED");
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid signature - Request rejected", null));
            }
            
            log.info("✅ Signature verified successfully!");
            
            PaymentResponse response = paymentService.processCallback(provider, callbackData);
            
            // Audit logging for successful callback
            Map<String, Object> callbackDetails = new HashMap<>();
            callbackDetails.put("provider", provider);
            callbackDetails.put("status", response.getStatus());
            callbackDetails.put("transactionId", transactionId);
            auditService.log("CALLBACK_RECEIVED", null, "system", "PAYMENT", 
                null, callbackDetails, "SUCCESS");
            
            return ResponseEntity.ok(new ApiResponse(true, "Callback processed successfully", response));
            
        } catch (RuntimeException e) {
            log.error("Callback processing failed: {}", e.getMessage());
            
            // Audit logging for callback failure
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("provider", provider);
            errorDetails.put("error", e.getMessage());
            auditService.log("CALLBACK_FAILED", null, "system", "PAYMENT", 
                null, errorDetails, "FAILED");
            
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get payment methods
     * GET /api/payments/methods
     */
    @GetMapping("/methods")
    public ResponseEntity<ApiResponse> getPaymentMethods() {
        var methods = java.util.List.of(
            java.util.Map.of(
                "id", "MTN_MONEY", 
                "name", "MTN Mobile Money", 
                "ussd", "*126#",
                "icon", "mtn",
                "supported", true
            ),
            java.util.Map.of(
                "id", "ORANGE_MONEY", 
                "name", "Orange Money", 
                "ussd", "#150#",
                "icon", "orange",
                "supported", true
            ),
            java.util.Map.of(
                "id", "CARD", 
                "name", "Visa / Mastercard", 
                "icon", "card",
                "supported", false,
                "comingSoon", true
            ),
            java.util.Map.of(
                "id", "CASH", 
                "name", "Cash on Delivery", 
                "icon", "cash",
                "supported", true
            )
        );
        
        return ResponseEntity.ok(new ApiResponse(true, "Payment methods retrieved", methods));
    }
    
    /**
     * Get current authenticated user ID
     */
    private Long getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                String email = ((UserDetails) principal).getUsername();
                return userService.getUserIdByEmail(email);
            }
        } catch (Exception e) {
            log.debug("Could not get current user ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Get current authenticated user email
     */
    private String getCurrentUserEmail() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            }
        } catch (Exception e) {
            log.debug("Could not get current user email: {}", e.getMessage());
        }
        return "system";
    }
}