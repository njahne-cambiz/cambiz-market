package com.cambiz.market.repository;

import com.cambiz.market.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository  // ✅ ADD THIS ANNOTATION
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByPhone(String phone);
    
    Boolean existsByEmail(String email);
    
    Boolean existsByPhone(String phone);
    
    Optional<User> findByEmailOrPhone(String email, String phone);
}