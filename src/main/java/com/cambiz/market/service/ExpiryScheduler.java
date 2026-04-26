package com.cambiz.market.service;

import com.cambiz.market.model.Product;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpiryScheduler {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Runs every hour to expire featured listings
     */
    @Scheduled(fixedRate = 3600000) // Every 1 hour
    @Transactional
    public void expireFeaturedListings() {
        List<Product> allProducts = productRepository.findAll();
        int expiredCount = 0;
        
        for (Product p : allProducts) {
            if (p.getIsFeatured() != null && p.getIsFeatured() 
                && p.getFeaturedUntil() != null 
                && p.getFeaturedUntil().isBefore(LocalDateTime.now())) {
                p.setIsFeatured(false);
                p.setFeaturedUntil(null);
                productRepository.save(p);
                expiredCount++;
            }
        }
        
        if (expiredCount > 0) {
            System.out.println("✅ Expired " + expiredCount + " featured listings");
        }
    }

    /**
     * Runs every hour to expire premium subscriptions
     */
    @Scheduled(fixedRate = 3600000) // Every 1 hour
    @Transactional
    public void expirePremiumSubscriptions() {
        List<User> allUsers = userRepository.findAll();
        int expiredCount = 0;
        
        for (User u : allUsers) {
            if ("PREMIUM".equals(u.getAccountType()) 
                && u.getPremiumUntil() != null 
                && u.getPremiumUntil().isBefore(LocalDateTime.now())) {
                u.setAccountType("REGULAR");
                u.setPremiumUntil(null);
                u.setCommissionRate(5.0); // Reset to default commission
                userRepository.save(u);
                expiredCount++;
            }
        }
        
        if (expiredCount > 0) {
            System.out.println("✅ Expired " + expiredCount + " premium subscriptions");
        }
    }

    /**
     * Manual trigger - expire featured now
     */
    public int manualExpireFeatured() {
        List<Product> allProducts = productRepository.findAll();
        int expiredCount = 0;
        
        for (Product p : allProducts) {
            if (p.getIsFeatured() != null && p.getIsFeatured() 
                && p.getFeaturedUntil() != null 
                && p.getFeaturedUntil().isBefore(LocalDateTime.now())) {
                p.setIsFeatured(false);
                p.setFeaturedUntil(null);
                productRepository.save(p);
                expiredCount++;
            }
        }
        return expiredCount;
    }

    /**
     * Manual trigger - expire premium now
     */
    public int manualExpirePremium() {
        List<User> allUsers = userRepository.findAll();
        int expiredCount = 0;
        
        for (User u : allUsers) {
            if ("PREMIUM".equals(u.getAccountType()) 
                && u.getPremiumUntil() != null 
                && u.getPremiumUntil().isBefore(LocalDateTime.now())) {
                u.setAccountType("REGULAR");
                u.setPremiumUntil(null);
                u.setCommissionRate(5.0);
                userRepository.save(u);
                expiredCount++;
            }
        }
        return expiredCount;
    }
}