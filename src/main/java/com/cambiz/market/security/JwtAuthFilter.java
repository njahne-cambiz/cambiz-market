package com.cambiz.market.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        // Skip public/auth endpoints
        if (path.startsWith("/api/auth") || path.startsWith("/api/public")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = parseJwt(request);

            if (jwt != null && !jwt.isBlank()) {
                String username = jwtUtils.extractUsername(jwt);
                logger.debug("JWT found for user: {}", username);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.debug("User loaded: {}", userDetails.getUsername());

                    if (jwtUtils.validateToken(jwt, userDetails)) {
                        logger.debug("Token validated successfully for: {}", username);

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.info("User authenticated: {}", username);
                    } else {
                        logger.warn("Token validation failed for: {}", username);
                    }
                }
            } else {
                logger.debug("No JWT token found in request to: {}", path);
            }

        } catch (Exception e) {
            logger.error("JWT authentication error for path {}: {}", path, e.getMessage());
            logger.debug("Stack trace:", e);
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }
}