package com.cambiz.market.service;

import com.cambiz.market.model.Product;
import com.cambiz.market.model.Payment;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FeaturedService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Transactional
    public Map<String, Object> boostProduct(Long productId, Long sellerId, String duration) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("You can only boost your own products");
        }
        
        double price;
        LocalDateTime featuredUntil;
        switch (duration) {
            case "1week": price = 5000; featuredUntil = LocalDateTime.now().plusWeeks(1); break;
            case "1month": price = 18000; featuredUntil = LocalDateTime.now().plusMonths(1); break;
            case "3months": price = 50000; featuredUntil = LocalDateTime.now().plusMonths(3); break;
            default: throw new RuntimeException("Invalid duration");
        }
        
        Payment payment = new Payment();
        payment.setOrderId(productId);
        payment.setBuyerId(sellerId);
        payment.setAmount(BigDecimal.valueOf(price));
        payment.setCurrency("XAF");
        payment.setPaymentMethod(Payment.PaymentMethod.MTN_MONEY);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setTransactionId("FEAT-" + System.currentTimeMillis());
        paymentRepository.save(payment);
        
        product.setIsFeatured(true);
        product.setFeaturedUntil(featuredUntil);
        product.setFeaturedPaymentId(payment.getId());
        productRepository.save(product);
        productRepository.flush(); // Force save to database
        
        return Map.of("productId", productId, "price", price, "duration", duration,
            "featuredUntil", featuredUntil.toString(), "transactionId", payment.getTransactionId(),
            "ussdCode", "*126*1*1*" + (int)price + "#");
    }

    public List<Map<String, Object>> getFeaturedProducts() {
        List<Product> all = productRepository.findAll();
        List<Map<String, Object>> featured = new ArrayList<>();
        for (Product p : all) {
            if (p.getIsFeatured() != null && p.getIsFeatured()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("name", p.getName());
                map.put("price", p.getPrice());
                map.put("discountedPrice", p.getDiscountedPrice());
                map.put("stockQuantity", p.getStockQuantity());
                map.put("categoryName", p.getCategory() != null ? p.getCategory().getNameEn() : null);
                map.put("imageUrl", p.getImageUrl());
                map.put("isFeatured", true);
                featured.add(map);
            }
        }
        return featured;
    }
}