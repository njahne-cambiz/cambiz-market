package com.cambiz.market.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminDashboardResponse {
    // Platform Overview
    private PlatformStats today;
    private PlatformStats thisWeek;
    private PlatformStats thisMonth;
    private PlatformStats allTime;
    
    // Charts
    private Map<String, BigDecimal> revenueChart;
    private Map<String, Integer> ordersChart;
    private Map<String, Integer> newUsersChart;
    private Map<String, BigDecimal> commissionChart;
    
    // Recent Activity
    private List<TransactionSummary> recentTransactions;
    private List<UserSummary> recentUsers;
    private List<OrderSummary> pendingOrders;
    private List<DisputeSummary> openDisputes;
    private List<ProductSummary> pendingApprovals;
    
    // System Health
    private SystemHealth systemHealth;
    
    // Quick Actions
    private List<QuickAction> quickActions;
    
    @Data
    @Builder
    public static class PlatformStats {
        private BigDecimal revenue;
        private Integer orders;
        private Integer newUsers;
        private Integer newSellers;
        private BigDecimal commission;
        private BigDecimal averageOrderValue;
    }
    
    @Data
    @Builder
    public static class TransactionSummary {
        private String transactionId;
        private Long orderId;
        private String buyerName;
        private String sellerName;
        private BigDecimal amount;
        private String paymentMethod;
        private String status;
        private LocalDateTime timestamp;
    }
    
    @Data
    @Builder
    public static class UserSummary {
        private Long userId;
        private String name;
        private String email;
        private String userType;
        private LocalDateTime joinedAt;
        private Boolean isVerified;
    }
    
    @Data
    @Builder
    public static class OrderSummary {
        private Long orderId;
        private String orderNumber;
        private String buyerName;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime orderDate;
    }
    
    @Data
    @Builder
    public static class DisputeSummary {
        private Long disputeId;
        private Long orderId;
        private String buyerName;
        private String sellerName;
        private String reason;
        private String status;
        private LocalDateTime openedAt;
        private String priority; // LOW, MEDIUM, HIGH, URGENT
    }
    
    @Data
    @Builder
    public static class ProductSummary {
        private Long productId;
        private String productName;
        private String sellerName;
        private BigDecimal price;
        private LocalDateTime submittedAt;
        private Boolean needsReview;
    }
    
    @Data
    @Builder
    public static class SystemHealth {
        private String status; // HEALTHY, WARNING, CRITICAL
        private Integer activeUsers;
        private Integer pendingPayments;
        private Integer failedJobs;
        private Double responseTime;
        private String lastBackup;
    }
    
    @Data
    @Builder
    public static class QuickAction {
        private String title;
        private String icon;
        private String link;
        private String color;
        private Boolean requiresAttention;
    }
}