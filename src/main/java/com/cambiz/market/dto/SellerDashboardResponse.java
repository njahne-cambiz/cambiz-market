package com.cambiz.market.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SellerDashboardResponse {
    // Today's Stats
    private Integer todayOrders;
    private BigDecimal todayRevenue;
    private Integer todayVisitors;
    private Integer pendingFulfillment;
    
    // Overall Stats
    private Integer totalProducts;
    private Integer totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal availableForPayout;
    private Double averageRating;
    
    // Charts Data
    private Map<String, BigDecimal> revenueChart; // Last 7 days
    private Map<String, Integer> ordersChart;      // Last 7 days
    private Map<String, Integer> topProducts;       // Product name -> Sold count
    
    // Recent Activity
    private List<SellerOrderSummary> recentOrders;
    private List<LowStockAlert> lowStockAlerts;
    private List<CustomerMessage> unreadMessages;
    private List<MakolaRequest> pendingMakolaRequests;
    
    // Quick Actions
    private List<QuickAction> quickActions;
    
    @Data
    @Builder
    public static class SellerOrderSummary {
        private Long orderId;
        private String orderNumber;
        private String buyerName;
        private BigDecimal amount;
        private String status;
        private LocalDateTime orderDate;
        private Boolean needsFulfillment;
    }
    
    @Data
    @Builder
    public static class LowStockAlert {
        private Long productId;
        private String productName;
        private String productImage;
        private Integer currentStock;
        private Integer minimumStock;
        private Integer soldToday;
        private String alertLevel; // LOW, CRITICAL
    }
    
    @Data
    @Builder
    public static class CustomerMessage {
        private Long messageId;
        private String customerName;
        private String message;
        private LocalDateTime receivedAt;
        private Boolean isUnread;
    }
    
    @Data
    @Builder
    public static class MakolaRequest {
        private Long negotiationId;
        private String buyerName;
        private String productName;
        private BigDecimal originalPrice;
        private BigDecimal offeredPrice;
        private String message;
        private LocalDateTime receivedAt;
        private String expiresIn;
    }
    
    @Data
    @Builder
    public static class QuickAction {
        private String title;
        private String icon;
        private String link;
        private String color;
        private Boolean isPrimary;
    }
}