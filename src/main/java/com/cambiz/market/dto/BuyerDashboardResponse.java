package com.cambiz.market.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BuyerDashboardResponse {
    // Stats
    private Integer totalOrders;
    private Integer activeMakolaNegotiations;
    private Integer savedItems;
    private BigDecimal totalSpent;
    
    // Recent Activity
    private List<OrderSummary> recentOrders;
    private List<MakolaSummary> pendingNegotiations;
    private List<ProductSummary> recentlyViewed;
    private List<ProductSummary> recommendedForYou;
    
    // Quick Actions
    private List<QuickAction> quickActions;
    
    @Data
    @Builder
    public static class OrderSummary {
        private Long orderId;
        private String orderNumber;
        private String status;
        private BigDecimal totalAmount;
        private Integer itemCount;
        private String estimatedDelivery;
        private Boolean canTrack;
    }
    
    @Data
    @Builder
    public static class MakolaSummary {
        private Long negotiationId;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal originalPrice;
        private BigDecimal yourOffer;
        private BigDecimal sellerCounter;
        private String status;
        private String expiresIn;
    }
    
    @Data
    @Builder
    public static class ProductSummary {
        private Long productId;
        private String name;
        private String imageUrl;
        private BigDecimal price;
        private BigDecimal discountedPrice;
        private Double rating;
        private Integer reviewCount;
        private String sellerName;
        private Boolean inStock;
    }
    
    @Data
    @Builder
    public static class QuickAction {
        private String title;
        private String icon;
        private String link;
        private String color;
    }
}