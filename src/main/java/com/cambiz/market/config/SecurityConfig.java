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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthFilter jwtAuthFilter;
    
    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:8080",
            "https://cambiz.cm",
            "https://www.cambiz.cm"
        ));
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
                // ========== PUBLIC ENDPOINTS (No Token Required) ==========
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers("/api/payments/callback/**").permitAll()
                
                // ✅ ADDED: Payment methods - PUBLIC
                .requestMatchers(HttpMethod.GET, "/api/payments/methods").permitAll()
                
                // ✅ API welcome page
                .requestMatchers("/api").permitAll()
                
                // ✅ Dashboard HTML pages - PUBLIC
                .requestMatchers("/", "/dashboard", "/dashboard/**").permitAll()
                .requestMatchers("/home", "/index", "/about", "/contact").permitAll()
                
                // ✅ Static resources - PUBLIC
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/favicon.ico", "/error").permitAll()
                
                // ✅ Actuator health - PUBLIC
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/test", "/api/test/**").permitAll()
                
                // ========== API DASHBOARD ENDPOINTS (Require Authentication) ==========
                .requestMatchers("/api/dashboard/buyer").hasRole("BUYER")
                .requestMatchers("/api/dashboard/seller").hasRole("SELLER")
                .requestMatchers("/api/dashboard/admin").hasRole("ADMIN")
                .requestMatchers("/api/dashboard/quick-stats").authenticated()
                
                // ========== PROTECTED ENDPOINTS (Token Required) ==========
                // Cart endpoints
                .requestMatchers(HttpMethod.GET, "/api/cart/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/cart/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/cart/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/cart/**").authenticated()
                
                // Order endpoints
                .requestMatchers("/api/orders/**").authenticated()
                
                // Makola (Negotiation) endpoints
                .requestMatchers("/api/makola/**").authenticated()
                
                // Payment endpoints (except callbacks and methods)
                .requestMatchers("/api/payments/initiate", "/api/payments/status/**").authenticated()
                .requestMatchers("/api/payments/order/**").authenticated()
                
                // Product write operations (CREATE, UPDATE, DELETE)
                .requestMatchers(HttpMethod.POST, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/products/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").authenticated()
                
                // Category write operations (ADMIN ONLY)
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}