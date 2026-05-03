package com.cambiz.market.service;

import com.cambiz.market.model.NjangiDeal;
import com.cambiz.market.model.NjangiParticipant;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class NjangiDealService {
    
    private final ConcurrentHashMap<Long, NjangiDeal> deals = new ConcurrentHashMap<>();
    private long nextId = 1;
    
    public NjangiDeal createDeal(Long productId, String productName, Long sellerId, String sellerName,
                                  int minParticipants, int maxParticipants, double individualPrice, double regularPrice) {
        NjangiDeal deal = new NjangiDeal(nextId++, productId, productName, sellerId, sellerName,
                                          minParticipants, maxParticipants, individualPrice, regularPrice);
        deals.put(deal.getId(), deal);
        return deal;
    }
    
    public List<NjangiDeal> getActiveDeals() {
        return deals.values().stream()
                .filter(d -> "ACTIVE".equals(d.getStatus()))
                .collect(Collectors.toList());
    }
    
    public NjangiDeal getDeal(Long dealId) {
        return deals.get(dealId);
    }
    
    public NjangiDeal joinDeal(Long dealId, Long userId, String userName, String userPhone) {
        NjangiDeal deal = deals.get(dealId);
        if (deal == null) return null;
        if (!"ACTIVE".equals(deal.getStatus())) return null;
        
        // Check if already joined
        boolean alreadyJoined = deal.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
        if (alreadyJoined) return deal;
        
        deal.getParticipants().add(new NjangiParticipant(userId, userName, userPhone));
        
        // Check if filled
        if (deal.getParticipants().size() >= deal.getMinParticipants()) {
            deal.setStatus("FILLED");
        }
        
        return deal;
    }
    
    public List<NjangiDeal> getSellerDeals(Long sellerId) {
        return deals.values().stream()
                .filter(d -> d.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }
    
    public NjangiDeal updateDealStatus(Long dealId, String status) {
        NjangiDeal deal = deals.get(dealId);
        if (deal != null) {
            deal.setStatus(status);
        }
        return deal;
    }
}