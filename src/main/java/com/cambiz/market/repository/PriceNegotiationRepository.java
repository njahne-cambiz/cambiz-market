package com.cambiz.market.repository;

import com.cambiz.market.model.PriceNegotiation;
import com.cambiz.market.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceNegotiationRepository extends JpaRepository<PriceNegotiation, Long> {
    
    // Find active negotiations for a buyer
    List<PriceNegotiation> findByBuyerAndStatusIn(User buyer, List<PriceNegotiation.NegotiationStatus> statuses);
    
    // Find active negotiations for a seller
    List<PriceNegotiation> findByProductSellerAndStatusIn(User seller, List<PriceNegotiation.NegotiationStatus> statuses);
    
    // Find negotiation by buyer, product, and active status
    Optional<PriceNegotiation> findByBuyerAndProductIdAndStatusIn(
        User buyer, Long productId, List<PriceNegotiation.NegotiationStatus> statuses);
    
    // Get buyer's negotiation history
    Page<PriceNegotiation> findByBuyerOrderByCreatedAtDesc(User buyer, Pageable pageable);
    
    // Get seller's negotiation history
    Page<PriceNegotiation> findByProductSellerOrderByCreatedAtDesc(User seller, Pageable pageable);
    
    // Find expired negotiations
    List<PriceNegotiation> findByStatusAndExpiresAtBefore(
        PriceNegotiation.NegotiationStatus status, java.time.LocalDateTime now);
    
    // AI Analytics queries
    @Query("SELECT AVG(p.finalPrice / p.originalPrice) FROM PriceNegotiation p WHERE p.status = 'ACCEPTED' AND p.product.id = :productId")
    Double getAverageAcceptedPercentage(@Param("productId") Long productId);
    
    @Query("SELECT AVG(p.finalPrice / p.originalPrice) FROM PriceNegotiation p WHERE p.status = 'ACCEPTED' AND p.product.category.id = :categoryId")
    Double getCategoryAverageDiscount(@Param("categoryId") Long categoryId);
    
    @Query("SELECT COUNT(p) FROM PriceNegotiation p WHERE p.product.seller.id = :sellerId AND p.status = 'ACCEPTED'")
    Long countAcceptedNegotiations(@Param("sellerId") Long sellerId);
}
