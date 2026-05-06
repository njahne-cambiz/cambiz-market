package com.cambiz.market.service;

import com.cambiz.market.model.Payment;
import com.cambiz.market.model.Transaction;
import com.cambiz.market.model.TransactionStatus;
import com.cambiz.market.model.TransactionType;
import com.cambiz.market.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public Transaction createPurchaseTransaction(Long orderId, Long buyerId, Long sellerId, Double amount,
                                                  Payment.PaymentMethod paymentMethod) {
        double commissionRate = 0.05;
        Double platformFee = amount * commissionRate;
        Double netAmount = amount - platformFee;

        Transaction txn = new Transaction(
                orderId, buyerId, sellerId, amount, platformFee, netAmount,
                TransactionType.PURCHASE, paymentMethod, TransactionStatus.PENDING,
                "Payment for Order #" + orderId
        );

        return transactionRepository.save(txn);
    }

    public Page<Transaction> getBuyerTransactions(Long buyerId, int page, int size) {
        return transactionRepository.findByBuyerIdOrderByCreatedAtDesc(
                buyerId, PageRequest.of(page, size));
    }

    public Page<Transaction> getSellerTransactions(Long sellerId, int page, int size) {
        return transactionRepository.findBySellerIdOrderByCreatedAtDesc(
                sellerId, PageRequest.of(page, size));
    }

    public Map<String, Object> getSellerStats(Long sellerId) {
        Page<Transaction> allTxns = transactionRepository.findBySellerIdOrderByCreatedAtDesc(
                sellerId, PageRequest.of(0, 1000));
        List<Transaction> sellerTxns = allTxns.getContent();

        double totalSales = sellerTxns.stream()
                .filter(t -> t.getType() == TransactionType.PURCHASE)
                .mapToDouble(Transaction::getAmount).sum();
        double totalCommission = sellerTxns.stream()
                .filter(t -> t.getType() == TransactionType.PURCHASE)
                .mapToDouble(Transaction::getPlatformFee).sum();
        double totalPayout = sellerTxns.stream()
                .filter(t -> t.getType() == TransactionType.PURCHASE)
                .mapToDouble(Transaction::getNetAmount).sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTransactions", sellerTxns.size());
        stats.put("totalSales", totalSales);
        stats.put("totalCommission", totalCommission);
        stats.put("totalPayout", totalPayout);
        return stats;
    }
}