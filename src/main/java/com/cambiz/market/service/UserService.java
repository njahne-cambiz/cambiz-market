package com.cambiz.market.service;

import com.cambiz.market.dto.JwtResponse;
import com.cambiz.market.dto.RegisterRequest;
import com.cambiz.market.model.Role;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.RoleRepository;
import com.cambiz.market.repository.UserRepository;
import com.cambiz.market.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;  // ✅ ADDED: BCrypt Password Encoder

    // ✅ FIXED: Use BCrypt for password encoding
    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    // REGISTER METHOD
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered!");
        }

        if (request.getPhone() != null && !request.getPhone().isEmpty()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone number already registered!");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(encodePassword(request.getPassword()));  // ✅ Now uses BCrypt
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBusinessName(request.getBusinessName());

        if ("SELLER".equalsIgnoreCase(request.getUserType())) {
            user.setUserType(User.UserType.SELLER);
        } else {
            user.setUserType(User.UserType.BUYER);
        }

        if ("FR".equalsIgnoreCase(request.getLanguage())) {
            user.setLanguage(User.Language.FR);
        } else {
            user.setLanguage(User.Language.EN);
        }

        Role userRole;
        if (user.getUserType() == User.UserType.SELLER) {
            userRole = roleRepository.findByName(Role.ROLE_SELLER)
                    .orElseThrow(() -> new RuntimeException("Seller role not found!"));
        } else {
            userRole = roleRepository.findByName(Role.ROLE_BUYER)
                    .orElseThrow(() -> new RuntimeException("Buyer role not found!"));
        }

        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));
        user.setStatus(User.UserStatus.ACTIVE);

        return userRepository.save(user);
    }

    // LOGIN METHOD
    public JwtResponse login(String email, String phone, String password) {
        User user = null;
        if (email != null && !email.isEmpty()) {
            user = userRepository.findByEmail(email).orElse(null);
        } else if (phone != null && !phone.isEmpty()) {
            user = userRepository.findByPhone(phone).orElse(null);
        }

        if (user == null) {
            throw new RuntimeException("User not found!");
        }

        // ✅ FIXED: Use BCrypt password matching
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        String token = jwtUtils.generateToken(user.getEmail());

        return new JwtResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getUserType().name(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toList())
        );
    }

    // ✅ GET USER ID BY EMAIL
    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    // ✅ ADDED: Find user by email (returns full User object)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    // ✅ ADDED: Find user by phone
    public User findByPhone(String phone) {
        return userRepository.findByPhone(phone).orElse(null);
    }

    // ✅ ADDED: Check if email exists
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ✅ ADDED: Check if phone exists
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    // ✅ ADDED: Get user by ID
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // ✅ ADDED: Update user password
    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ✅ ADDED: Update user profile
    public User updateProfile(Long userId, String firstName, String lastName, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phone != null) user.setPhone(phone);
        
        return userRepository.save(user);
    }

    // ✅ ADDED: Get all sellers
    public java.util.List<User> getAllSellers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getUserType() == User.UserType.SELLER)
                .collect(Collectors.toList());
    }

    // ✅ ADDED: Get all buyers
    public java.util.List<User> getAllBuyers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getUserType() == User.UserType.BUYER)
                .collect(Collectors.toList());
    }
}