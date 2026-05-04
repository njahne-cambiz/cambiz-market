package com.cambiz.market.service;

import com.cambiz.market.model.Coupon;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CouponService {
    
    private final ConcurrentHashMap<String, Coupon> coupons = new ConcurrentHashMap<>();
    private long nextId = 1;
    
    public Coupon createCoupon(String code, double discountPercent, double maxDiscount, int maxUses,
                                Long sellerId, String sellerName, int expiryDays) {
        String upperCode = code.toUpperCase();
        if (coupons.containsKey(upperCode)) {
            throw new RuntimeException("Coupon code already exists: " + upperCode);
        }
        Coupon coupon = new Coupon(nextId++, upperCode, discountPercent, maxDiscount, maxUses,
                                    sellerId, sellerName, expiryDays);
        coupons.put(upperCode, coupon);
        return coupon;
    }
    
    public Coupon validateCoupon(String code) {
        Coupon coupon = coupons.get(code.toUpperCase());
        if (coupon == null) return null;
        if (!coupon.isValid()) {
            if (coupon.getUsedCount() >= coupon.getMaxUses()) coupon.setStatus("DEPLETED");
            else if (LocalDateTime.now().isAfter(coupon.getExpiresAt())) coupon.setStatus("EXPIRED");
            return null;
        }
        return coupon;
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