package com.cambiz.market.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "security.2fa.enabled", havingValue = "true", matchIfMissing = false)
public class TwoFactorAuthController {
    
    // ... rest of the code stays the same
}