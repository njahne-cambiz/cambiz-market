package com.cambiz.market.service;

import com.cambiz.market.dto.PaymentRequest;
import com.cambiz.market.dto.PaymentResponse;
import com.cambiz.market.dto.PaymentStatusResponse;
import com.cambiz.market.model.Payment;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.PaymentRepository;
import com.cambiz.market.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {
    
    private final List<PaymentProvider> paymentProviders;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;
    
    /**
     * Initiate payment
     */
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating payment for order: {} using {}", request.getOrderId(), request.getPaymentMethod());
        
        PaymentProvider provider = paymentProviders.stream()
            .filter(p -> p.supports(request.getPaymentMethod()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Unsupported payment method: " + request.getPaymentMethod()));
        
        PaymentResponse response = provider.initiatePayment(request);
        
        Payment payment = new Payment();
        payment.setTransactionId(response.getTransactionId());
        payment.setOrderId(request.getOrderId());
        payment.setBuyerId(getCurrentBuyerId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setPaymentMethod(Payment.PaymentMethod.valueOf(request.getPaymentMethod()));
        payment.setPhoneNumber(request.getPhoneNumber());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        
        paymentRepository.save(payment);
        
        log.info("Payment record saved: {}", response.getTransactionId());
        
        return response;
    }
    
    /**
     * Check payment status - FIXED: Returns enriched response with order and amount
     */
    public PaymentStatusResponse checkPaymentStatus(String transactionId) {
        log.info("Checking payment status for transaction: {}", transactionId);
        
        Payment payment = paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        PaymentProvider provider = paymentProviders.stream()
            .filter(p -> p.supports(payment.getPaymentMethod().name()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No provider found"));
        
        PaymentStatusResponse providerResponse = provider.checkPaymentStatus(transactionId);
        
        // ✅ FIXED: Enrich response with order and amount data
        PaymentStatusResponse enrichedResponse = PaymentStatusResponse.builder()
            .transactionId(providerResponse.getTransactionId())
            .orderId(payment.getOrderId())
            .status(providerResponse.getStatus())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .paymentMethod(payment.getPaymentMethod().name())
            .message(providerResponse.getMessage())
            .completedAt(providerResponse.getCompletedAt())
            .lastChecked(providerResponse.getLastChecked())
            .build();
        
        if ("SUCCESS".equals(providerResponse.getStatus())) {
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setCompletedAt(providerResponse.getCompletedAt());
            paymentRepository.save(payment);
            
            orderService.updateOrderPaymentStatus(payment.getOrderId(), "PAID");
            log.info("Order {} marked as PAID", payment.getOrderId());
        } else if ("FAILED".equals(providerResponse.getStatus())) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
        
        return enrichedResponse;
    }
    
    /**
     * Process payment callback from provider
     */
    public PaymentResponse processCallback(String paymentMethod, String callbackData) {
        log.info("Processing {} callback", paymentMethod);
        
        PaymentProvider provider = paymentProviders.stream()
            .filter(p -> p.supports(paymentMethod))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Unsupported payment method"));
        
        PaymentResponse response = provider.processCallback(callbackData);
        
        // If callback indicates success, update the payment
        if ("SUCCESS".equals(response.getStatus())) {
            // Parse transaction ID from callback data (implementation depends on provider)
            // For now, this is a placeholder
            log.info("Payment successful via callback");
        }
        
        return response;
    }
    
    /**
     * Get payment by order ID
     */
    public PaymentResponse getPaymentByOrder(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
        
        return PaymentResponse.fromPayment(payment);
    }
    
    /**
     * Get buyer's payment history
     */
    public List<PaymentResponse> getBuyerPayments(Long buyerId) {
        return paymentRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
            .stream()
            .map(PaymentResponse::fromPayment)
            .toList();
    }
    
    /**
     * Get current buyer's payment history
     */
    public List<PaymentResponse> getMyPayments() {
        Long buyerId = getCurrentBuyerId();
        return getBuyerPayments(buyerId);
    }
    
    /**
     * Get total revenue
     */
    public BigDecimal getTotalRevenue() {
        BigDecimal total = paymentRepository.getTotalSuccessfulPayments();
        return total != null ? total : BigDecimal.ZERO;
    }
    
    /**
     * Get revenue by payment method
     */
    public BigDecimal getRevenueByMethod(String method) {
        try {
            Payment.PaymentMethod paymentMethod = Payment.PaymentMethod.valueOf(method);
            BigDecimal total = paymentRepository.getTotalByPaymentMethod(paymentMethod);
            return total != null ? total : BigDecimal.ZERO;
        } catch (IllegalArgumentException e) {
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Get current authenticated buyer ID
     */
    private Long getCurrentBuyerId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            if (principal instanceof UserDetails) {
                String email = ((UserDetails) principal).getUsername();
                
                User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
                
                log.debug("Current buyer ID: {} for email: {}", user.getId(), email);
                return user.getId();
            }
            
            throw new RuntimeException("User not authenticated");
        } catch (Exception e) {
            log.error("Error getting current buyer ID: {}", e.getMessage());
            log.warn("Using default buyer ID 4 for testing");
            return 4L;
        }
    }
    
    /**
     * Check if payment exists for order
     */
    public boolean hasSuccessfulPayment(Long orderId) {
        return paymentRepository.existsByOrderIdAndStatus(orderId, Payment.PaymentStatus.SUCCESS);
    }
    
    /**
     * Get pending payments that need status check
     */
    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(Payment.PaymentStatus.PENDING);
    }
    
    /**
     * Manually update payment status (for admin)
     */
    public void manualUpdateStatus(String transactionId, String status) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setStatus(Payment.PaymentStatus.valueOf(status));
        if ("SUCCESS".equals(status)) {
            payment.setCompletedAt(java.time.LocalDateTime.now());
            orderService.updateOrderPaymentStatus(payment.getOrderId(), "PAID");
        }
        paymentRepository.save(payment);
        
        log.info("Payment {} manually updated to {}", transactionId, status);
    }
}