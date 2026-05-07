package com.cambiz.market.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsDTO {
    private long totalUsers;
    private long totalSellers;
    private long totalBuyers;
    private long totalOrders;
    private long totalProducts;
    private long premiumSellers;
    private double totalRevenue;
    private long pendingDisputes;
}