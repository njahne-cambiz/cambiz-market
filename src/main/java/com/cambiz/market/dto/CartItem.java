package com.cambiz.market.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long productId;
    private String productName;
    private Long sellerId;
    private String sellerName;
    private Double price;
    private Double discountedPrice;
    private Integer quantity;
    private String imageUrl;
    private Double subtotal;
    private Integer availableStock;
    private Boolean inStock;
    private Boolean isNegotiated;  // MAKOLA MODE: Flag for negotiated items
    
    // Helper method to check if item has a discount
    public boolean hasDiscount() {
        return discountedPrice != null && discountedPrice > 0 && discountedPrice < price;
    }
    
    // Helper method to get the effective price (discounted or negotiated)
    public Double getEffectivePrice() {
        if (discountedPrice != null && discountedPrice > 0 && discountedPrice < price) {
            return discountedPrice;
        }
        return price;
    }
    
    // Helper method to calculate savings amount
    public Double getSavings() {
        if (isNegotiated != null && isNegotiated) {
            return subtotal != null ? subtotal * 0.15 : 0.0;
        }
        if (hasDiscount()) {
            return (price - discountedPrice) * quantity;
        }
        return 0.0;
    }
    
    // Helper method to get savings percentage
    public Integer getSavingsPercentage() {
        if (hasDiscount() && price != null && price > 0) {
            return (int) Math.round((1 - discountedPrice / price) * 100);
        }
        if (isNegotiated != null && isNegotiated) {
            return 15;
        }
        return 0;
    }
    
    // Helper method to get display price (shows strike-through if discounted)
    public Double getDisplayOriginalPrice() {
        if (hasDiscount() || (isNegotiated != null && isNegotiated)) {
            return price;
        }
        return null;
    }
}