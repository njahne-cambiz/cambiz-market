package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.model.Product;
import com.cambiz.market.model.Payment;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse> boostProduct(@RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            Long sellerId = Long.valueOf(request.get("sellerId").toString());
            String duration = request.get("duration").toString();
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
            
            if (!product.getSeller().getId().equals(sellerId)) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "You can only boost your own products", null));
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
            payment.setAmount(new java.math.BigDecimal(price));
            payment.setCurrency("XAF");
            payment.setPaymentMethod(Payment.PaymentMethod.MTN_MONEY);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setTransactionId("FEAT-" + System.currentTimeMillis());
            paymentRepository.save(payment);
            
            product.setIsFeatured(true);
            product.setFeaturedUntil(featuredUntil);
            product.setFeaturedPaymentId(payment.getId());
            productRepository.save(product);
            
            return ResponseEntity.ok(new ApiResponse(true, 
                "Product boosted! Dial *126*1*1*" + (int)price + "# to pay", 
                Map.of("productId", productId, "price", price, "duration", duration,
                       "featuredUntil", featuredUntil.toString(), "transactionId", payment.getTransactionId(),
                       "ussdCode", "*126*1*1*" + (int)price + "#")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // DEBUG VERSION - Shows exactly what's happening
    @GetMapping
    public ResponseEntity<ApiResponse> getFeaturedProducts() {
        try {
            List<Product> all = productRepository.findAll();
            List<Map<String, Object>> featured = new ArrayList<>();
            int totalProducts = all.size();
            int featuredCount = 0;
            
            for (Product p : all) {
                Boolean isFt = p.getIsFeatured();
                if (isFt != null && isFt) {
                    featuredCount++;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("name", p.getName());
                    map.put("price", p.getPrice());
                    map.put("isFeatured", true);
                    featured.add(map);
                }
            }
            
            Map<String, Object> debug = new HashMap<>();
            debug.put("totalProducts", totalProducts);
            debug.put("featuredCount", featuredCount);
            debug.put("featured", featured);
            
            return ResponseEntity.ok(new ApiResponse(true, 
                "Found " + featuredCount + " featured out of " + totalProducts, debug));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse(false, "ERROR: " + e.getMessage(), null));
        }
    }

    @GetMapping("/status/{productId}")
    public ResponseEntity<ApiResponse> checkBoostStatus(@PathVariable Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Product not found", null));
        }
        boolean isActive = product.getIsFeatured() && product.getFeaturedUntil() != null 
                && product.getFeaturedUntil().isAfter(LocalDateTime.now());
        return ResponseEntity.ok(new ApiResponse(true, "Boost status retrieved", 
            Map.of("isFeatured", product.getIsFeatured(), "featuredUntil", 
                   product.getFeaturedUntil() != null ? product.getFeaturedUntil().toString() : null, 
                   "isActive", isActive)));
    }
}