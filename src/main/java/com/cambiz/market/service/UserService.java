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
    private PasswordEncoder passwordEncoder;

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

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
        user.setPassword(encodePassword(request.getPassword()));
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
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName(Role.ROLE_SELLER);
                        newRole.setDescription("Seller Role");
                        return roleRepository.save(newRole);
                    });
        } else {
            userRole = roleRepository.findByName(Role.ROLE_BUYER)
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName(Role.ROLE_BUYER);
                        newRole.setDescription("Buyer Role");
                        return roleRepository.save(newRole);
                    });
        }

        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));
        user.setStatus(User.UserStatus.ACTIVE);

        // Generate referral code
        String name = (user.getFirstName() != null ? user.getFirstName() : "USER").toUpperCase();
        String code = name.replaceAll("[^A-Z0-9]", "");
        if (code.length() > 4) code = code.substring(0, 4);
        // Will set after save to get ID
        user.setReferralCode(code);

        // Process referral code if provided
        if (request.getReferralCode() != null && !request.getReferralCode().isEmpty()) {
            user.setReferredByCode(request.getReferralCode());
        }

        user = userRepository.save(user);
        
        // Update referral code with actual ID
        user.setReferralCode(code + user.getId());
        
        // Process referral bonus
        if (request.getReferralCode() != null && !request.getReferralCode().isEmpty()) {
            User referrer = userRepository.findByReferralCode(request.getReferralCode()).orElse(null);
            if (referrer != null) {
                user.setWalletBalance(user.getWalletBalance() + 500.0); // 500 XAF signup bonus
                referrer.setWalletBalance(referrer.getWalletBalance() != null ? referrer.getWalletBalance() + 1000.0 : 1000.0); // 1000 XAF referral bonus
                userRepository.save(referrer);
            }
        }
        
        return userRepository.save(user);
    }

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

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        String token = jwtUtils.generateTokenWithRoles(
            user.getEmail(),
            user.getRoles().stream().map(Role::getName).collect(Collectors.toList()),
            user.getUserType().name()
        );

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

    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findByPhone(String phone) {
        return userRepository.findByPhone(phone).orElse(null);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User updateProfile(Long userId, String firstName, String lastName, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (phone != null) user.setPhone(phone);
        return userRepository.save(user);
    }

    public java.util.List<User> getAllSellers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getUserType() == User.UserType.SELLER)
                .collect(Collectors.toList());
    }

    public java.util.List<User> getAllBuyers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getUserType() == User.UserType.BUYER)
                .collect(Collectors.toList());
    }
}