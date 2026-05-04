package com.cambiz.market.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cambiz.market.model.Coupon;

@Service
public class CouponService {
    
    private final ConcurrentHashMap<String, Coupon> coupons = new ConcurrentHashMap<>();
    private long nextId = 1;
    
    public Coupon createCoupon(String code, double discountPercent, double maxDiscount, double minOrderAmount,
                                Long productId, String productName, int maxUses, Long sellerId, 
                                String sellerName, int expiryDays, boolean isWelcome) {
        String upperCode = code.toUpperCase();
        if (coupons.containsKey(upperCode)) {
            throw new RuntimeException("Coupon code already exists: " + upperCode);
        }
        Coupon coupon = new Coupon(nextId++, upperCode, discountPercent, maxDiscount, minOrderAmount,
                                    productId, productName, maxUses, sellerId, sellerName, expiryDays, isWelcome);
        coupons.put(upperCode, coupon);
        return coupon;
    }
    
    public String generateWelcomeCoupon(Long userId, String userName) {
        String code = "WELCOME" + userId;
        if (coupons.containsKey(code)) return code;
        Coupon coupon = new Coupon(nextId++, code, 10.0, 5000.0, 0, null, null, 1, 
                                    userId, userName, 30, true);
        coupons.put(code, coupon);
        return code;
    }
    
    public Coupon getCoupon(String code) {
        return coupons.get(code.toUpperCase());
    }
    
    public void useCoupon(String code) {
        Coupon coupon = coupons.get(code.toUpperCase());
        if (coupon != null) {
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            if (coupon.getUsedCount() >= coupon.getMaxUses()) {
                coupon.setStatus("DEPLETED");
            }
        }
    }
    
    public List<Coupon> getSellerCoupons(Long sellerId) {
        return coupons.values().stream()
                .filter(c -> c.getSellerId().equals(sellerId))
                .collect(Collectors.toList());
    }
}