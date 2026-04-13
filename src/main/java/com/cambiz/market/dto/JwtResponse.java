package com.cambiz.market.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type;
    private Long id;
    private String email;
    private String phone;
    private String userType;
    private List<String> roles;
    
    // Constructor for backward compatibility (if needed)
    public JwtResponse(String token, Long id, String email, String userType) {
        this.token = token;
        this.type = "Bearer";
        this.id = id;
        this.email = email;
        this.phone = null;
        this.userType = userType;
        this.roles = null;
    }
}