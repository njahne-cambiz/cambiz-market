package com.cambiz.market.service;

import com.cambiz.market.dto.WishlistResponse;
import com.cambiz.market.model.Product;
import com.cambiz.market.model.User;
import com.cambiz.market.model.WishlistItem;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.UserRepository;
import com.cambiz.market.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistService {
    
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    
    public WishlistService(WishlistRepository wishlistRepository,
                          UserRepository userRepository,
                          ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }
    
    @Transactional
    public String addToWishlist(Long userId, Long productId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            return "Product already in wishlist";
        }
        
        WishlistItem item = new WishlistItem(user, product);
        wishlistRepository.save(item);
        return "Added to wishlist";
    }
    
    @Transactional
    public String removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        return "Removed from wishlist";
    }
    
    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist(Long userId) {
        List<WishlistItem> items = wishlistRepository.findByUserId(userId);
        return items.stream().map(item -> {
            Product product = item.getProduct();
            String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) 
                ? product.getImageUrls().get(0) : null;
            
            // Safe seller name extraction with fallback chain
            String sellerName = "Unknown Seller";
            User seller = product.getSeller();
            if (seller != null) {
                if (seller.getBusinessName() != null && !seller.getBusinessName().isBlank()) {
                    sellerName = seller.getBusinessName();
                } else if (seller.getFirstName() != null && !seller.getFirstName().isBlank()) {
                    sellerName = seller.getFirstName();
                } else if (seller.getEmail() != null) {
                    sellerName = seller.getEmail();
                }
            }
            
            return new WishlistResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getDiscountedPrice(),
                imageUrl,
                product.getProductCondition() != null ? product.getProductCondition() : "NEW",
                sellerName,
                item.getAddedDate(),
                product.getStockQuantity() > 0
            );
        }).collect(Collectors.toList());
    }
    
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }
    
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserId(userId);
    }
}