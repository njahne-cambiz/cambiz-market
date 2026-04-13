package com.cambiz.market.service;

import com.cambiz.market.dto.PaymentRequest;
import com.cambiz.market.dto.PaymentResponse;
import com.cambiz.market.dto.PaymentStatusResponse;

public interface PaymentProvider {
    
    PaymentResponse initiatePayment(PaymentRequest request);
    
    PaymentStatusResponse checkPaymentStatus(String transactionId);
    
    PaymentResponse processCallback(String callbackData);
    
    boolean supports(String paymentMethod);
}