package com.cambiz.market.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60000; // 1 minute
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        
        String clientIp = getClientIp(request);
        String endpoint = request.getRequestURI();
        String key = clientIp + ":" + endpoint;
        
        long now = System.currentTimeMillis();
        
        RequestCounter counter = requestCounts.computeIfAbsent(key, k -> new RequestCounter(now));
        
        synchronized (counter) {
            if (now - counter.windowStart > WINDOW_MS) {
                counter.count = 0;
                counter.windowStart = now;
            }
            
            counter.count++;
            
            // Special limit for login endpoint
            int maxRequests = endpoint.contains("/auth/login") ? LOGIN_MAX_ATTEMPTS : MAX_REQUESTS_PER_MINUTE;
            
            if (counter.count > maxRequests) {
                log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, endpoint);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\",\"timestamp\":" + now + "}");
                return;
            }
            
            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(maxRequests - counter.count));
            response.setHeader("X-RateLimit-Reset", String.valueOf((counter.windowStart + WINDOW_MS) / 1000));
        }
        
        chain.doFilter(request, response);
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
    
    private static class RequestCounter {
        long windowStart;
        int count;
        
        RequestCounter(long start) {
            this.windowStart = start;
            this.count = 0;
        }
    }
}