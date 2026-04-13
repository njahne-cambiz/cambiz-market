package com.cambiz.market.service;

import com.cambiz.market.model.AuditLog;
import com.cambiz.market.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    @Async
    public void log(String action, Long userId, String userEmail, 
                    String entityType, Long entityId, Object details, String status) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setUserId(userId);
            auditLog.setUserEmail(userEmail);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDetails(details != null ? objectMapper.writeValueAsString(details) : null);
            auditLog.setStatus(status);
            
            // Get request details
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }
    
    public void logLoginSuccess(Long userId, String email) {
        log("LOGIN_SUCCESS", userId, email, "USER", userId, null, "SUCCESS");
    }
    
    public void logLoginFailed(String email, String reason) {
        Map<String, String> details = new HashMap<>();
        details.put("reason", reason);
        log("LOGIN_FAILED", null, email, "USER", null, details, "FAILED");
    }
    
    public void logPaymentInitiated(Long userId, String email, Long orderId, Long paymentId) {
        Map<String, Object> details = new HashMap<>();
        details.put("orderId", orderId);
        log("PAYMENT_INITIATED", userId, email, "PAYMENT", paymentId, details, "SUCCESS");
    }
    
    public void logPaymentSuccess(Long userId, String email, Long paymentId) {
        log("PAYMENT_SUCCESS", userId, email, "PAYMENT", paymentId, null, "SUCCESS");
    }
    
    public void logMakolaOffer(Long userId, String email, Long productId, Long negotiationId) {
        Map<String, Object> details = new HashMap<>();
        details.put("productId", productId);
        log("MAKOLA_OFFER", userId, email, "NEGOTIATION", negotiationId, details, "SUCCESS");
    }
    
    public void logCheckout(Long userId, String email, Long orderId) {
        log("CHECKOUT", userId, email, "ORDER", orderId, null, "SUCCESS");
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}