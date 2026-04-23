package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.model.User;
import com.cambiz.market.service.TwoFactorAuthService;
import com.cambiz.market.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "security.2fa.enabled", havingValue = "true", matchIfMissing = false)
public class TwoFactorAuthController {
    
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserService userService;
    
    // ... rest of the code stays the same
}