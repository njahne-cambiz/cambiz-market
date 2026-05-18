package com.cambiz.market.config;

import com.cambiz.market.security.JwtAuthFilter;
import com.cambiz.market.security.RateLimitingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    private String allowedOrigins = "http://localhost:3000,http://localhost:8080,https://cambiz-market.onrender.com";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // PUBLIC ENDPOINTS
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers("/api/payments/callback/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/payments/methods").permitAll()
                .requestMatchers("/api").permitAll()

                // Reviews - public
                .requestMatchers("/api/reviews/**").permitAll()

                // Wallet - public for testing
                .requestMatchers("/api/wallet/**").permitAll()

                // Premium - benefits PUBLIC, status PUBLIC
                .requestMatchers(HttpMethod.GET, "/api/premium/benefits").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/premium/status/**").permitAll()

                // Database migration - public
                .requestMatchers("/api/admin/db/**").permitAll()

                // Uploaded images - public read access
                .requestMatchers("/uploads/**").permitAll()
                
                // Njangi active deals - public
                .requestMatchers(HttpMethod.GET, "/api/njangi/active").permitAll()
                
                // Flash sales active - public
                .requestMatchers(HttpMethod.GET, "/api/flash-sales/active").permitAll()
                
                // Coupon validate - public
                .requestMatchers(HttpMethod.GET, "/api/coupons/validate/**").permitAll()

                // STATIC & PAGES
                .requestMatchers("/", "/dashboard/**", "/store/**", "/shop/**", "/product", "/home", "/index", "/about", "/contact").permitAll()
                .requestMatchers("/premium").permitAll()
                .requestMatchers("/register").permitAll()
                .requestMatchers("/wishlist").permitAll()
                .requestMatchers("/orders").permitAll()
                .requestMatchers("/cart").permitAll()
                .requestMatchers("/payment").permitAll()
                .requestMatchers("/upload-images").permitAll()
                .requestMatchers("/create-product").permitAll()
                .requestMatchers("/njangi").permitAll()
                .requestMatchers("/create-njangi").permitAll()
                .requestMatchers("/referral").permitAll()
                .requestMatchers("/create-flash-sale").permitAll()
                .requestMatchers("/create-coupon").permitAll()
                .requestMatchers("/transactions").permitAll()
                
                // ADMIN PANEL PAGES
                .requestMatchers("/admin", "/admin/", "/admin/**").permitAll()
                
                // STATIC RESOURCES
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/favicon.ico", "/error").permitAll()

                // HEALTH
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Order tracking pages - public
                .requestMatchers("/order-tracking", "/track").permitAll()

                // ========== ALL ADMIN API - PUBLIC FOR NOW ==========
                .requestMatchers("/api/admin/**").permitAll()

                // ========== EVERYTHING ELSE AUTHENTICATED ==========
                .requestMatchers("/api/cart/**").authenticated()
                .requestMatchers("/api/orders/**").authenticated()
                .requestMatchers("/api/makola/**").authenticated()
                .requestMatchers("/api/payments/**").authenticated()
                .requestMatchers("/api/njangi/**").authenticated()
                .requestMatchers("/api/referral/**").authenticated()
                .requestMatchers("/api/flash-sales/**").authenticated()
                .requestMatchers("/api/coupons/**").authenticated()
                .requestMatchers("/api/transactions/**").authenticated()
                .requestMatchers("/api/wishlist/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/premium/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/featured/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/categories/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").authenticated()

                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}