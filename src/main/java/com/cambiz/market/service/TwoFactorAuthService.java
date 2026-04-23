package com.cambiz.market.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "security.2fa.enabled", havingValue = "true", matchIfMissing = false)
public class TwoFactorAuthService {
    
    // Remove JavaMailSender dependency - it's not needed for now
    private final ConcurrentHashMap<String, TwoFactorCode> pendingCodes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    
    private static final int CODE_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 3;
    
    /**
     * Generate and send 2FA code to seller's email
     */
    public void sendVerificationCode(String email, String sellerName) {
        String code = String.format("%06d", random.nextInt(1000000));
        pendingCodes.put(email, new TwoFactorCode(code, LocalDateTime.now(), 0));
        log.info("2FA code for {}: {}", email, code);
        // Email sending disabled until mail server is configured
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
        
        if (storedCode.getAttempts() >= MAX_ATTEMPTS) {
            pendingCodes.remove(email);
            return false;
        }
        
        if (storedCode.getCreatedAt().plusMinutes(CODE_EXPIRY_MINUTES).isBefore(LocalDateTime.now())) {
            pendingCodes.remove(email);
            return false;
        }
        
        storedCode.incrementAttempts();
        
        if (storedCode.getCode().equals(code)) {
            pendingCodes.remove(email);
            log.info("2FA verification successful for: {}", email);
            return true;
        }
        
        return false;
    }
    
    public boolean hasPendingCode(String email) {
        TwoFactorCode code = pendingCodes.get(email);
        if (code == null) return false;
        if (code.getCreatedAt().plusMinutes(CODE_EXPIRY_MINUTES).isBefore(LocalDateTime.now())) {
            pendingCodes.remove(email);
            return false;
        }
        return code.getAttempts() < MAX_ATTEMPTS;
    }
    
    public void invalidateCode(String email) {
        pendingCodes.remove(email);
    }
    
    private static class TwoFactorCode {
        private final String code;
        private final LocalDateTime createdAt;
        private int attempts;
        
        TwoFactorCode(String code, LocalDateTime createdAt, int attempts) {
            this.code = code;
            this.createdAt = createdAt;
            this.attempts = attempts;
        }
        
        public String getCode() { return code; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public int getAttempts() { return attempts; }
        public void incrementAttempts() { this.attempts++; }
    }
}