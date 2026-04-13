package com.cambiz.market.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class LoginRequest {

    // Email is optional (user may login with phone)
    private String email;

    // Phone is optional (user may login with email)
    @Pattern(regexp = "^[0-9]{9,12}$", message = "Phone number must be between 9 and 12 digits")
    private String phone;

    // Password is required
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

}