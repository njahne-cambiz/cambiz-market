package com.cambiz.market.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class WebSecurityConfig {
    
    /**
     * Force HTTPS in production
     */
    @Bean
    public FilterRegistrationBean<HttpsEnforcerFilter> httpsEnforcerFilter() {
        FilterRegistrationBean<HttpsEnforcerFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpsEnforcerFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
    
    /**
     * Handle proxy headers (important for Render/Heroku)
     */
    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ForwardedHeaderFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }
    
    /**
     * Filter that redirects HTTP to HTTPS
     */
    public static class HttpsEnforcerFilter implements Filter {
        
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;
            
            // Check if request is already HTTPS or from localhost
            String proto = req.getHeader("X-Forwarded-Proto");
            boolean isSecure = "https".equalsIgnoreCase(proto) || req.isSecure();
            boolean isLocal = req.getServerName().equals("localhost") || 
                             req.getServerName().equals("127.0.0.1");
            
            if (!isSecure && !isLocal) {
                // Redirect to HTTPS
                String httpsUrl = "https://" + req.getServerName() + req.getRequestURI();
                if (req.getQueryString() != null) {
                    httpsUrl += "?" + req.getQueryString();
                }
                res.sendRedirect(httpsUrl);
                return;
            }
            
            // Add security headers
            res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("X-Frame-Options", "DENY");
            res.setHeader("X-XSS-Protection", "1; mode=block");
            res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            res.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            
            chain.doFilter(request, response);
        }
    }
}