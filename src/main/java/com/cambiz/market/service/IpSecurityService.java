package com.cambiz.market.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class IpSecurityService {
    
    private final ConcurrentHashMap<String, UserIpHistory> userIpHistory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FailedAttempt> failedLogins = new ConcurrentHashMap<>();
    private final Set<String> blockedIps = ConcurrentHashMap.newKeySet();
    
    private static final int MAX_FAILED_ATTEMPTS = 10;
    private static final long FAILED_ATTEMPT_WINDOW_MINUTES = 15;
    private static final int MAX_IPS_PER_USER = 5;
    
    // Suspicious countries list (ISO codes)
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
        "KP", "IR", "SY", "CU", "VE"
    );
    
    /**
     * Check if IP is blocked
     */
    public boolean isIpBlocked(String ip) {
        return blockedIps.contains(ip);
    }
    
    /**
     * Record successful login
     */
    public void recordSuccessfulLogin(String email, String ip, HttpServletRequest request) {
        UserIpHistory history = userIpHistory.computeIfAbsent(email, k -> new UserIpHistory());
        history.addIp(ip, request);
        failedLogins.remove(ip);
        log.info("Successful login for {} from IP: {}", email, ip);
    }
    
    /**
     * Record failed login attempt
     */
    public void recordFailedLogin(String email, String ip) {
        FailedAttempt attempt = failedLogins.computeIfAbsent(ip, k -> new FailedAttempt());
        attempt.increment();
        
        if (attempt.getCount() >= MAX_FAILED_ATTEMPTS) {
            blockedIps.add(ip);
            log.warn("IP {} blocked due to {} failed login attempts", ip, attempt.getCount());
        }
        
        log.warn("Failed login attempt for {} from IP: {} (Attempt {}/{})", 
            email, ip, attempt.getCount(), MAX_FAILED_ATTEMPTS);
    }
    
    /**
     * Check if login is suspicious
     */
    public SuspiciousCheckResult checkSuspicious(String email, String ip, HttpServletRequest request) {
        UserIpHistory history = userIpHistory.get(email);
        
        if (history == null || history.isEmpty()) {
            return SuspiciousCheckResult.ok();
        }
        
        boolean isKnownIp = history.containsIp(ip);
        String country = getCountryFromIp(ip);
        boolean isHighRiskCountry = HIGH_RISK_COUNTRIES.contains(country);
        boolean tooManyIps = history.size() >= MAX_IPS_PER_USER;
        boolean rapidLocationChange = history.hasRapidLocationChange(ip);
        
        if (!isKnownIp || isHighRiskCountry || tooManyIps || rapidLocationChange) {
            List<String> reasons = new ArrayList<>();
            if (!isKnownIp) reasons.add("New login location");
            if (isHighRiskCountry) reasons.add("High-risk country");
            if (tooManyIps) reasons.add("Too many different locations");
            if (rapidLocationChange) reasons.add("Rapid location change detected");
            
            log.warn("Suspicious login detected for {}: {}", email, reasons);
            return SuspiciousCheckResult.suspicious(reasons);
        }
        
        return SuspiciousCheckResult.ok();
    }
    
    private String getCountryFromIp(String ip) {
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("192.168.")) {
            return "CM";
        }
        return "UNKNOWN";
    }
    
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(FAILED_ATTEMPT_WINDOW_MINUTES);
        failedLogins.entrySet().removeIf(entry -> 
            entry.getValue().getFirstAttempt().isBefore(cutoff) && entry.getValue().getCount() < MAX_FAILED_ATTEMPTS);
        userIpHistory.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    public void unblockIp(String ip) {
        blockedIps.remove(ip);
        failedLogins.remove(ip);
        log.info("IP {} manually unblocked", ip);
    }
    
    public Set<String> getBlockedIps() {
        return new HashSet<>(blockedIps);
    }
    
    // ==================== INNER CLASSES ====================
    
    private static class UserIpHistory {
        private final Map<String, IpRecord> knownIps = new HashMap<>();
        
        boolean isEmpty() {
            return knownIps.isEmpty();
        }
        
        int size() {
            return knownIps.size();
        }
        
        boolean containsIp(String ip) {
            return knownIps.containsKey(ip);
        }
        
        void addIp(String ip, HttpServletRequest request) {
            knownIps.put(ip, new IpRecord(ip, LocalDateTime.now(), request));
        }
        
        boolean hasRapidLocationChange(String newIp) {
            if (knownIps.isEmpty()) return false;
            LocalDateTime now = LocalDateTime.now();
            return knownIps.values().stream()
                .anyMatch(r -> r.getLastSeen().isAfter(now.minusHours(1)) && !r.getIp().equals(newIp));
        }
        
        boolean isExpired() {
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            return knownIps.values().stream()
                .allMatch(r -> r.getLastSeen().isBefore(threeMonthsAgo));
        }
    }
    
    @lombok.Getter
    private static class IpRecord {
        private final String ip;
        private final LocalDateTime firstSeen;
        private final LocalDateTime lastSeen;
        private final String userAgent;
        
        IpRecord(String ip, LocalDateTime seen, HttpServletRequest request) {
            this.ip = ip;
            this.firstSeen = seen;
            this.lastSeen = seen;
            this.userAgent = request.getHeader("User-Agent");
        }
    }
    
    @lombok.Getter
    private static class FailedAttempt {
        private int count = 0;
        private final LocalDateTime firstAttempt = LocalDateTime.now();
        
        void increment() {
            count++;
        }
    }
    
    @lombok.Getter
    public static class SuspiciousCheckResult {
        private final boolean suspicious;
        private final List<String> reasons;
        
        private SuspiciousCheckResult(boolean suspicious, List<String> reasons) {
            this.suspicious = suspicious;
            this.reasons = reasons != null ? reasons : new ArrayList<>();
        }
        
        public static SuspiciousCheckResult ok() {
            return new SuspiciousCheckResult(false, new ArrayList<>());
        }
        
        public static SuspiciousCheckResult suspicious(List<String> reasons) {
            return new SuspiciousCheckResult(true, reasons);
        }
    }
}