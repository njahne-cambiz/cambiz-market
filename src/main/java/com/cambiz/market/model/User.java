package com.cambiz.market.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "email"),
           @UniqueConstraint(columnNames = "phone")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private String businessName;

    @Enumerated(EnumType.STRING)
    private UserType userType = UserType.BUYER;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private Language language = Language.EN;

    @Column(name = "account_type")
    private String accountType = "REGULAR";

    @Column(name = "premium_until")
    private LocalDateTime premiumUntil;

    @Column(name = "commission_rate")
    private Double commissionRate = 5.0;

    @Column(name = "wallet_balance")
    private Double walletBalance = 0.0;

    @Column(name = "total_earned")
    private Double totalEarned = 0.0;

    @Column(name = "referral_code", unique = true)
    private String referralCode;

    @Column(name = "referred_by_code")
    private String referredByCode;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
               joinColumns = @JoinColumn(name = "user_id"),
               inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum UserType { BUYER, SELLER, ADMIN }
    public enum UserStatus { PENDING, ACTIVE, SUSPENDED, BANNED }
    public enum Language { EN, FR }
}