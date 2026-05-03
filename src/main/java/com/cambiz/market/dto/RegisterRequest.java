package com.cambiz.market.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[0-9]{9}$", message = "Phone must be 9 digits")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String firstName;
    private String lastName;
    private String businessName;

    @NotBlank(message = "User type is required")
    @Pattern(regexp = "BUYER|SELLER", message = "User type must be BUYER or SELLER")
    private String userType;

    private String language = "EN";
    
    private String referralCode;
}