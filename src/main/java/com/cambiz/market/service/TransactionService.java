package com.cambiz.market.service;

import com.cambiz.market.model.Transaction;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    
    private final ConcurrentHashMap<Long, Transaction> transactions = new ConcurrentHashMap<>();
    private long nextId = 1;
    
    public Transaction recordTransaction(Long orderId, String orderNumber, Long buyerId, String buyerName,
                                          Long sellerId, String sellerName, String productName, double orderAmount,
                                          double commission, double sellerPayout, String type, String paymentMethod) {
        Transaction transaction = new Transaction(nextId++, orderId, orderNumber, buyerId, buyerName,
                sellerId, sellerName, productName, orderAmount, commission, sellerPayout, type, paymentMethod);
        transactions.put(transaction.getId(), transaction);
        return transaction;
    }
    
    public List<Transaction> getSellerTransactions(Long sellerId) {
        return transactions.values().stream()
                .filter(t -> t.getSellerId().equals(sellerId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }
    
    public List<Transaction> getBuyerTransactions(Long buyerId) {
        return transactions.values().stream()
                .filter(t -> t.getBuyerId().equals(buyerId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }
    
    public List<Transaction> getAllTransactions() {
        return transactions.values().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }
    
    public Map<String, Object> getSellerStats(Long sellerId) {
        List<Transaction> sellerTxns = getSellerTransactions(sellerId);
        double totalSales = sellerTxns.stream().mapToDouble(Transaction::getOrderAmount).sum();
        double totalCommission = sellerTxns.stream().mapToDouble(Transaction::getCommission).sum();
        double totalPayout = sellerTxns.stream().mapToDouble(Transaction::getSellerPayout).sum();
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTransactions", sellerTxns.size());
        stats.put("totalSales", totalSales);
        stats.put("totalCommission", totalCommission);
        stats.put("totalPayout", totalPayout);
        return stats;
    }
}