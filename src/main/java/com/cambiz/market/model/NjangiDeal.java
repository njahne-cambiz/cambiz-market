package com.cambiz.market.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NjangiDeal {
    private Long id;
    private Long productId;
    private String productName;
    private Long sellerId;
    private String sellerName;
    private int minParticipants;
    private int maxParticipants;
    private double individualPrice;
    private double regularPrice;
    private double savingsPerPerson;
    private String status; // ACTIVE, FILLED, COMPLETED, CANCELLED
    private List<NjangiParticipant> participants = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    
    public NjangiDeal() {}
    
    public NjangiDeal(Long id, Long productId, String productName, Long sellerId, String sellerName,
                      int minParticipants, int maxParticipants, double individualPrice, double regularPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.minParticipants = minParticipants;
        this.maxParticipants = maxParticipants;
        this.individualPrice = individualPrice;
        this.regularPrice = regularPrice;
        this.savingsPerPerson = regularPrice - individualPrice;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(7);
    }
    
    // Getters and Setters
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
    public int getMinParticipants() { return minParticipants; }
    public void setMinParticipants(int minParticipants) { this.minParticipants = minParticipants; }
    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
    public double getIndividualPrice() { return individualPrice; }
    public void setIndividualPrice(double individualPrice) { this.individualPrice = individualPrice; }
    public double getRegularPrice() { return regularPrice; }
    public void setRegularPrice(double regularPrice) { this.regularPrice = regularPrice; }
    public double getSavingsPerPerson() { return savingsPerPerson; }
    public void setSavingsPerPerson(double savingsPerPerson) { this.savingsPerPerson = savingsPerPerson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<NjangiParticipant> getParticipants() { return participants; }
    public void setParticipants(List<NjangiParticipant> participants) { this.participants = participants; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public int getParticipantCount() { return participants.size(); }
    public boolean isFilled() { return participants.size() >= minParticipants; }
    public int getRemainingSpots() { return minParticipants - participants.size(); }
}