package com.cambiz.market.model;

import java.time.LocalDateTime;

public class NjangiParticipant {
    private Long userId;
    private String userName;
    private String userPhone;
    private String paymentStatus; // PENDING, PAID
    private LocalDateTime joinedAt;
    
    public NjangiParticipant() {}
    
    public NjangiParticipant(Long userId, String userName, String userPhone) {
        this.userId = userId;
        this.userName = userName;
        this.userPhone = userPhone;
        this.paymentStatus = "PENDING";
        this.joinedAt = LocalDateTime.now();
    }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}