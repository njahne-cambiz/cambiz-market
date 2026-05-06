package com.cambiz.market.model;

import java.time.LocalDateTime;

public class Transaction {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private String productName;
    private double orderAmount;
    private double commission;
    private double sellerPayout;
    private String type; // SALE, COMMISSION, PAYOUT, REFUND, COUPON_DISCOUNT
    private String status; // COMPLETED, PENDING, FAILED
    private String paymentMethod;
    private LocalDateTime createdAt;
    
    public Transaction() {}
    
    public Transaction(Long id, Long orderId, String orderNumber, Long buyerId, String buyerName,
                       Long sellerId, String sellerName, String productName, double orderAmount,
                       double commission, double sellerPayout, String type, String paymentMethod) {
        this.id = id;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.productName = productName;
        this.orderAmount = orderAmount;
        this.commission = commission;
        this.sellerPayout = sellerPayout;
        this.type = type;
        this.status = "COMPLETED";
        this.paymentMethod = paymentMethod;
        this.createdAt = LocalDateTime.now();
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public double getOrderAmount() { return orderAmount; }
    public void setOrderAmount(double orderAmount) { this.orderAmount = orderAmount; }
    public double getCommission() { return commission; }
    public void setCommission(double commission) { this.commission = commission; }
    public double getSellerPayout() { return sellerPayout; }
    public void setSellerPayout(double sellerPayout) { this.sellerPayout = sellerPayout; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}