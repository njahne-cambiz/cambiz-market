package com.cambiz.market.service;

import com.cambiz.market.model.User;
import com.cambiz.market.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReferralService {
    
    private final UserRepository userRepository;
    private final ConcurrentHashMap<String, List<Long>> referrals = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Double> commissions = new ConcurrentHashMap<>();
    
    public ReferralService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public String getReferralCode(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        return user.getReferralCode();
    }
    
    public Map<String, Object> getReferralStats(Long userId) {
        String code = getReferralCode(userId);
        if (code == null) {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("referralCode", "N/A");
            stats.put("totalReferrals", 0);
            stats.put("totalEarned", 0.0);
            stats.put("referralLink", "https://cambiz-market.onrender.com/register");
            return stats;
        }
        
        List<Long> referredUsers = referrals.getOrDefault(code, new ArrayList<>());
        double totalEarned = commissions.getOrDefault(userId, 0.0);
        
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("referralCode", code);
        stats.put("totalReferrals", referredUsers.size());
        stats.put("totalEarned", totalEarned);
        stats.put("referralLink", "https://cambiz-market.onrender.com/register?ref=" + code);
        return stats;
    }
}