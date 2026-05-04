package com.cambiz.market.model;

import java.time.LocalDateTime;

public class FlashSale {
    private Long id;
    private Long productId;
    private String productName;
    private Long sellerId;
    private String sellerName;
    private double originalPrice;
    private double flashPrice;
    private int discountPercent;
    private int totalStock;
    private int soldCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // ACTIVE, ENDED, CANCELLED
    
    public FlashSale() {}
    
    public FlashSale(Long id, Long productId, String productName, Long sellerId, String sellerName,
                     double originalPrice, double flashPrice, int totalStock, int durationHours) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.originalPrice = originalPrice;
        this.flashPrice = flashPrice;
        this.discountPercent = (int)Math.round((1 - flashPrice/originalPrice) * 100);
        this.totalStock = totalStock;
        this.soldCount = 0;
        this.startTime = LocalDateTime.now();
        this.endTime = LocalDateTime.now().plusHours(durationHours);
        this.status = "ACTIVE";
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }
    public double getFlashPrice() { return flashPrice; }
    public void setFlashPrice(double flashPrice) { this.flashPrice = flashPrice; }
    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }
    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }
    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRemainingStock() { return totalStock - soldCount; }
    public int getSoldPercent() { return totalStock > 0 ? (int)((soldCount * 100.0) / totalStock) : 0; }
    public boolean isActive() { return "ACTIVE".equals(status) && LocalDateTime.now().isBefore(endTime) && getRemainingStock() > 0; }
    public String getTimeLeft() {
        if (!isActive()) return "Ended";
        long seconds = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        if (seconds <= 0) return "Ended";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m left";
    }
}