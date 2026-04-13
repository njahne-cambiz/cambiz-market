package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.model.User;
import com.cambiz.market.service.TwoFactorAuthService;
import com.cambiz.market.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TwoFactorAuthController {
    
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserService userService;
    
    /**
     * Request 2FA code for seller
     * POST /api/auth/2fa/request
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse> requestCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        try {
            // Verify user exists
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "User not found", null));
            }
            
            // Check if user is a seller
            if (user.getUserType() == null || !"SELLER".equals(user.getUserType().name())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "2FA is only required for sellers", null));
            }
            
            // Build seller name
            String sellerName = "Seller";
            if (user.getFirstName() != null) {
                sellerName = user.getFirstName();
                if (user.getLastName() != null) {
                    sellerName = user.getFirstName() + " " + user.getLastName();
                }
            }
            
            twoFactorAuthService.sendVerificationCode(email, sellerName);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", email);
            responseData.put("expiresIn", 10);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Verification code sent to your email",
                responseData
            ));
            
        } catch (Exception e) {
            log.error("Failed to send 2FA code: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * Verify 2FA code
     * POST /api/auth/2fa/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        
        if (email == null || code == null) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Email and code are required", null));
        }
        
        boolean verified = twoFactorAuthService.verifyCode(email, code);
        
        if (verified) {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", email);
            responseData.put("verified", true);
            
            return ResponseEntity.ok(new ApiResponse(true, "Verification successful", responseData));
        } else {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Invalid or expired code", null));
        }
    }
    
    /**
     * Check if 2FA is pending for email
     * GET /api/auth/2fa/status/{email}
     */
    @GetMapping("/status/{email}")
    public ResponseEntity<ApiResponse> checkStatus(@PathVariable String email) {
        boolean hasPending = twoFactorAuthService.hasPendingCode(email);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("email", email);
        responseData.put("pendingVerification", hasPending);
        
        return ResponseEntity.ok(new ApiResponse(true, "Status retrieved", responseData));
    }
    
    /**
     * Resend 2FA code
     * POST /api/auth/2fa/resend
     */
    @PostMapping("/resend")
    public ResponseEntity<ApiResponse> resendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        try {
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "User not found", null));
            }
            
            // Invalidate old code
            twoFactorAuthService.invalidateCode(email);
            
            // Build seller name
            String sellerName = "Seller";
            if (user.getFirstName() != null) {
                sellerName = user.getFirstName();
                if (user.getLastName() != null) {
                    sellerName = user.getFirstName() + " " + user.getLastName();
                }
            }
            
            // Send new code
            twoFactorAuthService.sendVerificationCode(email, sellerName);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("email", email);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "New verification code sent to your email",
                responseData
            ));
            
        } catch (Exception e) {
            log.error("Failed to resend 2FA code: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
}