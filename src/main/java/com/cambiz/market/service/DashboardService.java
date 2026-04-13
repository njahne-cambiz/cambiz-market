package com.cambiz.market.service;

import com.cambiz.market.dto.*;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {
    
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    
    /**
     * BUYER DASHBOARD
     */
    public BuyerDashboardResponse getBuyerDashboard() {
        getCurrentUserId();
        
        // Quick Actions
        List<BuyerDashboardResponse.QuickAction> quickActions = Arrays.asList(
            BuyerDashboardResponse.QuickAction.builder()
                .title("Continue Shopping")
                .icon("🛍️")
                .link("/products")
                .color("blue")
                .build(),
            BuyerDashboardResponse.QuickAction.builder()
                .title("Track Orders")
                .icon("📦")
                .link("/my-orders")
                .color("green")
                .build(),
            BuyerDashboardResponse.QuickAction.builder()
                .title("Makola Deals")
                .icon("💰")
                .link("/makola")
                .color("orange")
                .build(),
            BuyerDashboardResponse.QuickAction.builder()
                .title("Saved Items")
                .icon("❤️")
                .link("/wishlist")
                .color("red")
                .build()
        );
        
        // Recent negotiations
        List<BuyerDashboardResponse.MakolaSummary> negotiations = getBuyerNegotiations();
        
        // Recommended products
        List<BuyerDashboardResponse.ProductSummary> recommended = getRecommendedProducts();
        
        // Recently viewed (from session/cache)
        List<BuyerDashboardResponse.ProductSummary> recentViews = getRecentlyViewed();
        
        return BuyerDashboardResponse.builder()
            .totalOrders(getBuyerOrderCount())
            .activeMakolaNegotiations(negotiations.size())
            .savedItems(0) // Implement wishlist
            .totalSpent(getBuyerTotalSpent())
            .recentOrders(getBuyerRecentOrders())
            .pendingNegotiations(negotiations)
            .recentlyViewed(recentViews)
            .recommendedForYou(recommended)
            .quickActions(quickActions)
            .build();
    }
    
    /**
     * SELLER DASHBOARD
     */
    public SellerDashboardResponse getSellerDashboard() {
        getCurrentUserId();
        
        // Quick Actions
        List<SellerDashboardResponse.QuickAction> quickActions = Arrays.asList(
            SellerDashboardResponse.QuickAction.builder()
                .title("Add New Product")
                .icon("➕")
                .link("/products/new")
                .color("green")
                .isPrimary(true)
                .build(),
            SellerDashboardResponse.QuickAction.builder()
                .title("Manage Inventory")
                .icon("📦")
                .link("/inventory")
                .color("blue")
                .isPrimary(false)
                .build(),
            SellerDashboardResponse.QuickAction.builder()
                .title("View Orders")
                .icon("🛒")
                .link("/seller/orders")
                .color("purple")
                .isPrimary(false)
                .build(),
            SellerDashboardResponse.QuickAction.builder()
                .title("Respond to Offers")
                .icon("💬")
                .link("/makola/seller")
                .color("orange")
                .isPrimary(false)
                .build()
        );
        
        // Low stock alerts
        List<SellerDashboardResponse.LowStockAlert> lowStockAlerts = getLowStockAlerts();
        
        // Pending Makola requests
        List<SellerDashboardResponse.MakolaRequest> makolaRequests = getPendingMakolaRequests();
        
        // Recent orders
        List<SellerDashboardResponse.SellerOrderSummary> recentOrders = getSellerRecentOrders();
        
        // Revenue chart data
        Map<String, BigDecimal> revenueChart = getSellerRevenueChart();
        Map<String, Integer> ordersChart = getSellerOrdersChart();
        Map<String, Integer> topProducts = getSellerTopProducts();
        
        return SellerDashboardResponse.builder()
            .todayOrders(getSellerTodayOrders())
            .todayRevenue(getSellerTodayRevenue())
            .todayVisitors(0) // Implement analytics
            .pendingFulfillment(getSellerPendingFulfillment())
            .totalProducts(getSellerProductCount())
            .totalOrders(getSellerTotalOrders())
            .totalRevenue(getSellerTotalRevenue())
            .availableForPayout(getSellerAvailablePayout())
            .averageRating(getSellerAverageRating())
            .revenueChart(revenueChart)
            .ordersChart(ordersChart)
            .topProducts(topProducts)
            .recentOrders(recentOrders)
            .lowStockAlerts(lowStockAlerts)
            .unreadMessages(new ArrayList<>()) // Implement messaging
            .pendingMakolaRequests(makolaRequests)
            .quickActions(quickActions)
            .build();
    }
    
    /**
     * ADMIN DASHBOARD
     */
    public AdminDashboardResponse getAdminDashboard() {
        // Platform stats
        AdminDashboardResponse.PlatformStats today = getPlatformStats();
        AdminDashboardResponse.PlatformStats thisWeek = getPlatformStats();
        AdminDashboardResponse.PlatformStats thisMonth = getPlatformStats();
        AdminDashboardResponse.PlatformStats allTime = getPlatformStats();
        
        // Quick Actions
        List<AdminDashboardResponse.QuickAction> quickActions = Arrays.asList(
            AdminDashboardResponse.QuickAction.builder()
                .title("Review New Products")
                .icon("🔍")
                .link("/admin/products/pending")
                .color("orange")
                .requiresAttention(true)
                .build(),
            AdminDashboardResponse.QuickAction.builder()
                .title("Process Payouts")
                .icon("💰")
                .link("/admin/payouts")
                .color("green")
                .requiresAttention(false)
                .build(),
            AdminDashboardResponse.QuickAction.builder()
                .title("Resolve Disputes")
                .icon("⚖️")
                .link("/admin/disputes")
                .color("red")
                .requiresAttention(true)
                .build(),
            AdminDashboardResponse.QuickAction.builder()
                .title("View Reports")
                .icon("📊")
                .link("/admin/reports")
                .color("blue")
                .requiresAttention(false)
                .build()
        );
        
        // System Health
        AdminDashboardResponse.SystemHealth health = AdminDashboardResponse.SystemHealth.builder()
            .status("HEALTHY")
            .activeUsers(getActiveUserCount())
            .pendingPayments(getPendingPaymentCount())
            .failedJobs(0)
            .responseTime(245.0)
            .lastBackup(LocalDateTime.now().minusHours(6).toString())
            .build();
        
        return AdminDashboardResponse.builder()
            .today(today)
            .thisWeek(thisWeek)
            .thisMonth(thisMonth)
            .allTime(allTime)
            .revenueChart(getAdminRevenueChart())
            .ordersChart(getAdminOrdersChart())
            .newUsersChart(getAdminUsersChart())
            .commissionChart(getAdminCommissionChart())
            .recentTransactions(getRecentTransactions())
            .recentUsers(getRecentUsers())
            .pendingOrders(getPendingOrders())
            .openDisputes(getOpenDisputes())
            .pendingApprovals(getPendingProductApprovals())
            .systemHealth(health)
            .quickActions(quickActions)
            .build();
    }
    
    /**
     * QUICK STATS - For header/mobile
     */
    public Map<String, Object> getQuickStats() {
        Long userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElse(null);
        
        Map<String, Object> stats = new HashMap<>();
        
        if (user != null) {
            if ("BUYER".equals(user.getUserType().name())) {
                stats.put("cartItems", cartService.getCart(userId).getItemCount());
                stats.put("activeNegotiations", getBuyerNegotiations().size());
                stats.put("unreadMessages", 0);
            } else if ("SELLER".equals(user.getUserType().name())) {
                stats.put("pendingOrders", getSellerPendingFulfillment());
                stats.put("todayRevenue", getSellerTodayRevenue());
                stats.put("lowStockItems", getLowStockAlerts().size());
            }
        }
        
        return stats;
    }
    
    // ========== HELPER METHODS ==========
    
    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String email = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return userRepository.findByEmail(email).orElseThrow().getId();
        }
        return 4L; // Default for testing
    }
    
    private Integer getBuyerOrderCount() {
        return 5; // Placeholder
    }
    
    private BigDecimal getBuyerTotalSpent() {
        return new BigDecimal("1250000");
    }
    
    private List<BuyerDashboardResponse.OrderSummary> getBuyerRecentOrders() {
        return new ArrayList<>();
    }
    
    private List<BuyerDashboardResponse.MakolaSummary> getBuyerNegotiations() {
        return new ArrayList<>();
    }
    
    private List<BuyerDashboardResponse.ProductSummary> getRecommendedProducts() {
        return new ArrayList<>();
    }
    
    private List<BuyerDashboardResponse.ProductSummary> getRecentlyViewed() {
        return new ArrayList<>();
    }
    
    private Integer getSellerTodayOrders() {
        return 3;
    }
    
    private BigDecimal getSellerTodayRevenue() {
        return new BigDecimal("1950000");
    }
    
    private Integer getSellerPendingFulfillment() {
        return 2;
    }
    
    private Integer getSellerProductCount() {
        return productRepository.findAll().size();
    }
    
    private Integer getSellerTotalOrders() {
        return 45;
    }
    
    private BigDecimal getSellerTotalRevenue() {
        return new BigDecimal("8750000");
    }
    
    private BigDecimal getSellerAvailablePayout() {
        return new BigDecimal("3500000");
    }
    
    private Double getSellerAverageRating() {
        return 4.7;
    }
    
    private Map<String, BigDecimal> getSellerRevenueChart() {
        Map<String, BigDecimal> chart = new LinkedHashMap<>();
        chart.put("Mon", new BigDecimal("250000"));
        chart.put("Tue", new BigDecimal("180000"));
        chart.put("Wed", new BigDecimal("320000"));
        chart.put("Thu", new BigDecimal("280000"));
        chart.put("Fri", new BigDecimal("450000"));
        chart.put("Sat", new BigDecimal("350000"));
        chart.put("Sun", new BigDecimal("120000"));
        return chart;
    }
    
    private Map<String, Integer> getSellerOrdersChart() {
        Map<String, Integer> chart = new LinkedHashMap<>();
        chart.put("Mon", 5);
        chart.put("Tue", 3);
        chart.put("Wed", 7);
        chart.put("Thu", 6);
        chart.put("Fri", 9);
        chart.put("Sat", 8);
        chart.put("Sun", 2);
        return chart;
    }
    
    private Map<String, Integer> getSellerTopProducts() {
        Map<String, Integer> top = new LinkedHashMap<>();
        top.put("iPhone 15 Pro", 12);
        top.put("Samsung S24", 8);
        top.put("MacBook Air", 5);
        top.put("AirPods Pro", 15);
        return top;
    }
    
    private List<SellerDashboardResponse.LowStockAlert> getLowStockAlerts() {
        return new ArrayList<>();
    }
    
    private List<SellerDashboardResponse.MakolaRequest> getPendingMakolaRequests() {
        return new ArrayList<>();
    }
    
    private List<SellerDashboardResponse.SellerOrderSummary> getSellerRecentOrders() {
        return new ArrayList<>();
    }
    
    private AdminDashboardResponse.PlatformStats getPlatformStats() {
        return AdminDashboardResponse.PlatformStats.builder()
            .revenue(new BigDecimal("15000000"))
            .orders(230)
            .newUsers(45)
            .newSellers(8)
            .commission(new BigDecimal("750000"))
            .averageOrderValue(new BigDecimal("65217"))
            .build();
    }
    
    private Map<String, BigDecimal> getAdminRevenueChart() {
        return new LinkedHashMap<>();
    }
    
    private Map<String, Integer> getAdminOrdersChart() {
        return new LinkedHashMap<>();
    }
    
    private Map<String, Integer> getAdminUsersChart() {
        return new LinkedHashMap<>();
    }
    
    private Map<String, BigDecimal> getAdminCommissionChart() {
        return new LinkedHashMap<>();
    }
    
    private List<AdminDashboardResponse.TransactionSummary> getRecentTransactions() {
        return new ArrayList<>();
    }
    
    private List<AdminDashboardResponse.UserSummary> getRecentUsers() {
        return new ArrayList<>();
    }
    
    private List<AdminDashboardResponse.OrderSummary> getPendingOrders() {
        return new ArrayList<>();
    }
    
    private List<AdminDashboardResponse.DisputeSummary> getOpenDisputes() {
        return new ArrayList<>();
    }
    
    private List<AdminDashboardResponse.ProductSummary> getPendingProductApprovals() {
        return new ArrayList<>();
    }
    
    private Integer getActiveUserCount() {
        return 42;
    }
    
    private Integer getPendingPaymentCount() {
        return 8;
    }
    
    public Object getSellerAnalytics(String period) {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("period", period);
        analytics.put("revenue", getSellerRevenueChart());
        analytics.put("orders", getSellerOrdersChart());
        analytics.put("topProducts", getSellerTopProducts());
        return analytics;
    }
    
    public Object generateReport(String type, String format) {
        Map<String, Object> report = new HashMap<>();
        report.put("type", type);
        report.put("format", format);
        report.put("generatedAt", LocalDateTime.now());
        report.put("data", "Report data would be here");
        return report;
    }
}