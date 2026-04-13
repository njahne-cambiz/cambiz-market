package com.cambiz.market.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorAuthService {
    
    private final JavaMailSender mailSender;
    private final ConcurrentHashMap<String, TwoFactorCode> pendingCodes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    
    @Value("${spring.mail.username:noreply@cambiz.cm}")
    private String fromEmail;
    
    private static final int CODE_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 3;
    
    /**
     * Generate and send 2FA code to seller's email
     */
    public void sendVerificationCode(String email, String sellerName) {
        String code = String.format("%06d", random.nextInt(1000000));
        
        pendingCodes.put(email, new TwoFactorCode(code, LocalDateTime.now(), 0));
        
        // Send email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("CamBiz Seller Verification Code");
            message.setText(String.format("""
                Dear %s,
                
                Your CamBiz Seller verification code is: %s
                
                This code will expire in %d minutes.
                
                If you did not request this code, please ignore this email.
                
                Thank you for selling on CamBiz Market!
                
                ---
                CamBiz Security Team
                """, sellerName, code, CODE_EXPIRY_MINUTES));
            
            mailSender.send(message);
            log.info("2FA code sent to: {}", email);
            
        } catch (Exception e) {
            log.error("Failed to send 2FA email: {}", e.getMessage());
            pendingCodes.remove(email);
            throw new RuntimeException("Failed to send verification code. Please try again.");
        }
    }
    
    /**
     * Verify the 2FA code
     */
    public boolean verifyCode(String email, String code) {
        TwoFactorCode storedCode = pendingCodes.get(email);
        
        if (storedCode == null) {
            log.warn("No pending 2FA code for: {}", email);
            return false;
        }
        
        // Check attempts
        if (storedCode.getAttempts() >= MAX_ATTEMPTS) {
            pendingCodes.remove(email);
            log.warn("Max 2FA attempts exceeded for: {}", email);
            return false;
        }
        
        // Check expiry
        if (storedCode.getCreatedAt().plusMinutes(CODE_EXPIRY_MINUTES).isBefore(LocalDateTime.now())) {
            pendingCodes.remove(email);
            log.warn("2FA code expired for: {}", email);
            return false;
        }
        
        storedCode.incrementAttempts();
        
        // Verify code
        if (storedCode.getCode().equals(code)) {
            pendingCodes.remove(email);
            log.info("2FA verification successful for: {}", email);
            return true;
        }
        
        log.warn("Invalid 2FA code for: {} (Attempt {}/{})", email, storedCode.getAttempts(), MAX_ATTEMPTS);
        return false;
    }
    
    /**
     * Check if email has a pending 2FA code
     */
    public boolean hasPendingCode(String email) {
        TwoFactorCode code = pendingCodes.get(email);
        if (code == null) return false;
        
        // Check expiry
        if (code.getCreatedAt().plusMinutes(CODE_EXPIRY_MINUTES).isBefore(LocalDateTime.now())) {
            pendingCodes.remove(email);
            return false;
        }
        
        return code.getAttempts() < MAX_ATTEMPTS;
    }
    
    /**
     * Invalidate any pending code for email
     */
    public void invalidateCode(String email) {
        pendingCodes.remove(email);
        log.debug("2FA code invalidated for: {}", email);
    }
    
    /**
     * Inner class for storing 2FA codes
     */
    private static class TwoFactorCode {
        private final String code;
        private final LocalDateTime createdAt;
        private int attempts;
        
        TwoFactorCode(String code, LocalDateTime createdAt, int attempts) {
            this.code = code;
            this.createdAt = createdAt;
            this.attempts = attempts;
        }
        
        public String getCode() {
            return code;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public int getAttempts() {
            return attempts;
        }
        
        public void incrementAttempts() {
            this.attempts++;
        }
    }
}