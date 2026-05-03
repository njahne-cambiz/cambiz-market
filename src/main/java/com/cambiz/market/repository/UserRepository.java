package com.cambiz.market.repository;

import com.cambiz.market.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByPhone(String phone);
    
    Optional<User> findByReferralCode(String referralCode);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
}