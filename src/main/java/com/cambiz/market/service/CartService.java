package com.cambiz.market.service;

import com.cambiz.market.dto.CartItem;
import com.cambiz.market.dto.CartResponse;
import com.cambiz.market.model.Product;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CartService {
    
    // In-memory cart storage (key: userId, value: CartResponse)
    private final ConcurrentHashMap<Long, CartResponse> userCarts = new ConcurrentHashMap<>();
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Add item to cart with regular price
     */
    public CartResponse addToCart(Long userId, Long productId, Integer quantity) {
        // 1. Validate product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Use regular price
        return addToCartWithCustomPrice(userId, productId, quantity, product.getPrice());
    }
    
    /**
     * Add item to cart with custom negotiated price (MAKOLA MODE)
     */
    public CartResponse addToCartWithCustomPrice(Long userId, Long productId, Integer quantity, Double customPrice) {
        // 1. Validate product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // 2. Get or create cart
        CartResponse cart = userCarts.getOrDefault(userId, new CartResponse());
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        }
        
        // 3. Check if product already in cart
        CartItem existingItem = null;
        for (CartItem item : cart.getItems()) {
            if (item.getProductId().equals(productId)) {
                existingItem = item;
                break;
            }
        }
        
        // 4. Get seller info
        String sellerName = "Seller";
        Long sellerId = product.getSellerId() != null ? product.getSellerId() : 1L;
        try {
            User seller = userRepository.findById(sellerId).orElse(null);
            if (seller != null) {
                String firstName = null;
                String lastName = null;
                String email = null;
                
                try {
                    firstName = (String) seller.getClass().getMethod("getFirstName").invoke(seller);
                } catch (Exception e1) {
                    // Method doesn't exist
                }
                
                try {
                    lastName = (String) seller.getClass().getMethod("getLastName").invoke(seller);
                } catch (Exception e1) {
                    // Method doesn't exist
                }
                
                try {
                    email = (String) seller.getClass().getMethod("getEmail").invoke(seller);
                } catch (Exception e1) {
                    // Method doesn't exist
                }
                
                // Build seller name from available data
                if (firstName != null && lastName != null) {
                    sellerName = firstName + " " + lastName;
                } else if (firstName != null) {
                    sellerName = firstName;
                } else if (email != null) {
                    sellerName = email.split("@")[0];
                } else {
                    sellerName = "Seller #" + sellerId;
                }
            }
        } catch (Exception e) {
            sellerName = "Seller #" + sellerId;
        }
        
        // 5. Add or update item
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            // Use existing price (could be negotiated or regular)
            existingItem.setSubtotal(existingItem.getPrice() * existingItem.getQuantity());
        } else {
            CartItem newItem = new CartItem();
            newItem.setProductId(product.getId());
            newItem.setProductName(product.getName());
            newItem.setSellerId(sellerId);
            newItem.setSellerName(sellerName);
            newItem.setPrice(customPrice); // Use custom price
            newItem.setDiscountedPrice(customPrice);
            newItem.setQuantity(quantity);
            newItem.setImageUrl(product.getImageUrl());
            newItem.setSubtotal(customPrice * quantity);
            newItem.setIsNegotiated(customPrice < product.getPrice()); // Flag if discounted
            
            cart.getItems().add(newItem);
        }
        
        // 6. Update totals
        updateCartTotals(cart);
        userCarts.put(userId, cart);
        
        return cart;
    }
    
    /**
     * Remove item from cart
     */
    public CartResponse removeFromCart(Long userId, Long productId) {
        CartResponse cart = userCarts.get(userId);
        if (cart == null || cart.getItems() == null) {
            return new CartResponse();
        }
        
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        updateCartTotals(cart);
        userCarts.put(userId, cart);
        
        return cart;
    }
    
    /**
     * Update item quantity
     */
    public CartResponse updateQuantity(Long userId, Long productId, Integer quantity) {
        if (quantity <= 0) {
            return removeFromCart(userId, productId);
        }
        
        CartResponse cart = userCarts.get(userId);
        if (cart == null || cart.getItems() == null) {
            cart = new CartResponse();
            cart.setItems(new ArrayList<>());
            userCarts.put(userId, cart);
            return cart;
        }
        
        for (CartItem item : cart.getItems()) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(quantity);
                item.setSubtotal(item.getPrice() * quantity);
                break;
            }
        }
        
        updateCartTotals(cart);
        userCarts.put(userId, cart);
        
        return cart;
    }
    
    /**
     * Get user cart
     */
    public CartResponse getCart(Long userId) {
        CartResponse cart = userCarts.get(userId);
        if (cart == null) {
            cart = new CartResponse();
            cart.setItems(new ArrayList<>());
        }
        return cart;
    }
    
    /**
     * Clear cart
     */
    public void clearCart(Long userId) {
        userCarts.remove(userId);
    }
    
    /**
     * Check if cart has negotiated items
     */
    public boolean hasNegotiatedItems(Long userId) {
        CartResponse cart = userCarts.get(userId);
        if (cart == null || cart.getItems() == null) {
            return false;
        }
        
        for (CartItem item : cart.getItems()) {
            if (item.getIsNegotiated() != null && item.getIsNegotiated()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Calculate total savings from negotiations
     */
    public Double calculateTotalSavings(Long userId) {
        CartResponse cart = userCarts.get(userId);
        if (cart == null || cart.getItems() == null) {
            return 0.0;
        }
        
        double savings = 0.0;
        for (CartItem item : cart.getItems()) {
            if (item.getIsNegotiated() != null && item.getIsNegotiated()) {
                // Need original price - for now estimate based on typical discount
                savings += item.getSubtotal() * 0.15; // Approximate 15% savings
            }
        }
        return savings;
    }
    
    /**
     * Calculate cart totals
     */
    private void updateCartTotals(CartResponse cart) {
        if (cart.getItems() == null) {
            cart.setSubtotal(0.0);
            cart.setTotal(0.0);
            cart.setItemCount(0);
            return;
        }
        
        double subtotal = 0.0;
        for (CartItem item : cart.getItems()) {
            if (item.getSubtotal() != null) {
                subtotal += item.getSubtotal();
            }
        }
        
        cart.setSubtotal(subtotal);
        cart.setDeliveryFee(0.0);
        cart.setTotal(subtotal);
        cart.setItemCount(cart.getItems().size());
    }
}