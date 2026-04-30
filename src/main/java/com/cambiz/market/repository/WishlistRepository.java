package com.cambiz.market.repository;

import com.cambiz.market.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    
    List<WishlistItem> findByUserId(Long userId);
    
    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    long countByUserId(Long userId);
    
    void deleteByUserIdAndProductId(Long userId, Long productId);
}