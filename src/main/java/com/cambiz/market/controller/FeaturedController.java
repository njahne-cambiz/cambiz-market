package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.dto.ProductResponse;
import com.cambiz.market.model.Product;
import com.cambiz.market.model.Payment;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/featured")
@CrossOrigin(origins = "*")
public class FeaturedController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostMapping("/boost")
    @Transactional
    public ResponseEntity<ApiResponse> boostProduct(@RequestBody Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        Long sellerId = Long.valueOf(request.get("sellerId").toString());
        String duration = request.get("duration").toString();
        
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Product not found", null));
        }
        
        double price;
        LocalDateTime featuredUntil;
        switch (duration) {
            case "1week": price = 5000; featuredUntil = LocalDateTime.now().plusWeeks(1); break;
            case "1month": price = 18000; featuredUntil = LocalDateTime.now().plusMonths(1); break;
            case "3months": price = 50000; featuredUntil = LocalDateTime.now().plusMonths(3); break;
            default: return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid duration", null));
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
        productRepository.saveAndFlush(product);
        
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("price", price);
        data.put("duration", duration);
        data.put("featuredUntil", featuredUntil.toString());
        data.put("transactionId", payment.getTransactionId());
        data.put("ussdCode", "*126*1*1*" + (int)price + "#");
        
        return ResponseEntity.ok(new ApiResponse(true, "Product boosted!", data));
    }

    @GetMapping
    @Transactional
    public ResponseEntity<ApiResponse> getFeaturedProducts() {
        List<Product> all = productRepository.findAll();
        List<ProductResponse> featured = new ArrayList<>();
        
        for (Product p : all) {
            if (p.getIsFeatured() != null && p.getIsFeatured()) {
                featured.add(ProductResponse.fromProduct(p));
            }
        }
        return ResponseEntity.ok(new ApiResponse(true, "Found " + featured.size() + " featured", featured));
    }
}