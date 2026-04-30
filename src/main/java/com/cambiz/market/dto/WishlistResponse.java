package com.cambiz.market.dto;

import java.time.LocalDateTime;

public class WishlistResponse {
    
    private Long wishlistItemId;
    private Long productId;
    private String productName;
    private String productDescription;
    private Double price;
    private Double discountedPrice;
    private String imageUrl;
    private String condition;
    private String sellerBusinessName;
    private LocalDateTime addedDate;
    private boolean inStock;
    
    public WishlistResponse(Long wishlistItemId, Long productId, String productName,
                           String productDescription, Double price, Double discountedPrice,
                           String imageUrl, String condition, String sellerBusinessName,
                           LocalDateTime addedDate, boolean inStock) {
        this.wishlistItemId = wishlistItemId;
        this.productId = productId;
        this.productName = productName;
        this.productDescription = productDescription;
        this.price = price;
        this.discountedPrice = discountedPrice;
        this.imageUrl = imageUrl;
        this.condition = condition;
        this.sellerBusinessName = sellerBusinessName;
        this.addedDate = addedDate;
        this.inStock = inStock;
    }
    
    public Long getWishlistItemId() { return wishlistItemId; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductDescription() { return productDescription; }
    public Double getPrice() { return price; }
    public Double getDiscountedPrice() { return discountedPrice; }
    public String getImageUrl() { return imageUrl; }
    public String getCondition() { return condition; }
    public String getSellerBusinessName() { return sellerBusinessName; }
    public LocalDateTime getAddedDate() { return addedDate; }
    public boolean isInStock() { return inStock; }
}