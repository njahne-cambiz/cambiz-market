package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.dto.JwtResponse;
import com.cambiz.market.dto.LoginRequest;
import com.cambiz.market.dto.RegisterRequest;
import com.cambiz.market.model.User;
import com.cambiz.market.service.UserService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(request);
            
            // Fixed: Using 3-parameter constructor (success, message, data)
            ApiResponse response = new ApiResponse(
                    true,
                    "User registered successfully!",
                    user
            );

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (RuntimeException e) {
            // Fixed: Using 2-parameter pattern with null data
            ApiResponse response = new ApiResponse(
                    false,
                    e.getMessage(),
                    null
            );

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            JwtResponse jwtResponse = userService.login(
                    request.getEmail(),
                    request.getPhone(),
                    request.getPassword()
            );

            // Wrap JwtResponse in ApiResponse for consistency
            ApiResponse response = new ApiResponse(
                    true,
                    "Login successful",
                    jwtResponse
            );
            
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            ApiResponse error = new ApiResponse(
                    false,
                    e.getMessage(),
                    null
            );

            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        }
    }
    
    // Optional: Add a logout endpoint if needed
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {
        ApiResponse response = new ApiResponse(
                true,
                "Logged out successfully",
                null
        );
        return ResponseEntity.ok(response);
    }
}