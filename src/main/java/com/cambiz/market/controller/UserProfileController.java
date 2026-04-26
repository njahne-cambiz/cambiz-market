package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/profile")
@CrossOrigin(origins = "*")
public class UserProfileController {

    @Autowired
    private UserRepository userRepository;

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse> updateProfile(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found", null));
        }
        
        if (request.containsKey("firstName")) user.setFirstName(request.get("firstName"));
        if (request.containsKey("lastName")) user.setLastName(request.get("lastName"));
        if (request.containsKey("businessName")) user.setBusinessName(request.get("businessName"));
        if (request.containsKey("phone")) user.setPhone(request.get("phone"));
        
        userRepository.save(user);
        
        return ResponseEntity.ok(new ApiResponse(true, "Profile updated", Map.of(
            "id", user.getId(),
            "firstName", user.getFirstName(),
            "lastName", user.getLastName(),
            "businessName", user.getBusinessName(),
            "email", user.getEmail()
        )));
    }
}
