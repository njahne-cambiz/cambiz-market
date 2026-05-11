package com.cambiz.market.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cambiz.market.model.Product;
import com.cambiz.market.model.User;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Active products for public store
    Page<Product> findByIsActiveTrue(Pageable pageable);
    
    // Seller's products
    List<Product> findBySeller(User seller);
    
    // ✅ FIXED: Use @Query with seller.id instead of sellerId
    @Query("SELECT p FROM Product p WHERE p.seller.id = :sellerId")
    List<Product> findBySellerId(@Param("sellerId") Long sellerId);
    
    // Products by category
    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);
    
    // Search products
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description, Pageable pageable);
    
    // Featured products
    List<Product> findTop10ByIsFeaturedTrueOrderByCreatedAtDesc();
    
    // Total count
    long count();
    
    // ========== PRODUCT APPROVAL QUERIES ==========
    
    // Find all pending products
    @Query("SELECT p FROM Product p WHERE p.isApproved = false OR p.isApproved IS NULL ORDER BY p.createdAt DESC")
    List<Product> findByIsApprovedFalseOrIsApprovedNull();
    
    // Find all approved products
    @Query("SELECT p FROM Product p WHERE p.isApproved = true ORDER BY p.approvedAt DESC")
    List<Product> findByIsApprovedTrue();
    
    // Find rejected products
    @Query("SELECT p FROM Product p WHERE (p.isApproved = false OR p.isApproved IS NULL) AND p.rejectionReason IS NOT NULL ORDER BY p.rejectedAt DESC")
    List<Product> findByIsApprovedFalseAndRejectionReasonNotNull();
    
    // Count pending products
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isApproved = false OR p.isApproved IS NULL")
    long countByIsApprovedFalseOrIsApprovedNull();
    
    // Count by status
    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = :status")
    long countByStatus(@Param("status") Product.ProductStatus status);
    
    // Find by status
    @Query("SELECT p FROM Product p WHERE p.status = :status ORDER BY p.createdAt DESC")
    List<Product> findByStatus(@Param("status") Product.ProductStatus status);
    
    // ========== APPROVED PRODUCTS FOR PUBLIC STORE ==========
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isApproved = true ORDER BY p.createdAt DESC")
    Page<Product> findActiveAndApproved(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.isFeatured = true AND p.isApproved = true AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findTop10ByIsFeaturedTrueAndApprovedOrderByCreatedAtDesc();
    
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true AND p.isApproved = true")
    Page<Product> findByCategoryIdAndActiveAndApproved(@Param("categoryId") Long categoryId, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isApproved = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> searchApprovedProducts(@Param("keyword") String keyword, Pageable pageable);
}