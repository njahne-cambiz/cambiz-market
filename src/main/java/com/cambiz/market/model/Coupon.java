package com.cambiz.market.model;

import java.time.LocalDateTime;

public class Coupon {
    private Long id;
    private String code;
    private double discountPercent;
    private double maxDiscount;
    private int maxUses;
    private int usedCount;
    private Long sellerId;
    private String sellerName;
    private LocalDateTime expiresAt;
    private String status; // ACTIVE, EXPIRED, DEPLETED
    
    public Coupon() {}
    
    public Coupon(Long id, String code, double discountPercent, double maxDiscount, int maxUses,
                  Long sellerId, String sellerName, int expiryDays) {
        this.id = id;
        this.code = code.toUpperCase();
        this.discountPercent = discountPercent;
        this.maxDiscount = maxDiscount;
        this.maxUses = maxUses;
        this.usedCount = 0;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.expiresAt = LocalDateTime.now().plusDays(expiryDays);
        this.status = "ACTIVE";
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }
    public double getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(double maxDiscount) { this.maxDiscount = maxDiscount; }
    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int maxUses) { this.maxUses = maxUses; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public boolean isValid() {
        return "ACTIVE".equals(status) && LocalDateTime.now().isBefore(expiresAt) && usedCount < maxUses;
    }
    
    public double calculateDiscount(double total) {
        double discount = total * (discountPercent / 100.0);
        return Math.min(discount, maxDiscount);
    }
}