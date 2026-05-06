package com.cambiz.market.model;

public enum TransactionType {
    PURCHASE,    // Buyer paying for an order
    PAYOUT,      // Platform sending money to Seller
    REFUND,      // Money sent back to Buyer
    COMMISSION   // Platform cut (Internal tracking)
}