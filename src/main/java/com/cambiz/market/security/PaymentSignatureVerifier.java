package com.cambiz.market.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
@Slf4j
public class PaymentSignatureVerifier {
    
    @Value("${mtn.api.secondary-key:test-secondary-key}")
    private String mtnSecretKey;
    
    @Value("${orange.api.client-secret:test-secret}")
    private String orangeSecretKey;
    
    /**
     * Verify MTN Money callback signature
     */
    public boolean verifyMTNSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(mtnSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] computedSignature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedBase64 = Base64.getEncoder().encodeToString(computedSignature);
            
            boolean valid = computedBase64.equals(signature);
            log.info("MTN signature verification: {}", valid ? "VALID" : "INVALID");
            return valid;
            
        } catch (Exception e) {
            log.error("MTN signature verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verify Orange Money callback signature
     */
    public boolean verifyOrangeSignature(String payload, String signature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = payload + orangeSecretKey;
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);
            
            boolean valid = computed.equalsIgnoreCase(signature);
            log.info("Orange signature verification: {}", valid ? "VALID" : "INVALID");
            return valid;
            
        } catch (Exception e) {
            log.error("Orange signature verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}