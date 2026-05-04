package com.cambiz.market.service;

import com.cambiz.market.model.FlashSale;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class FlashSaleService {
    
    private final ConcurrentHashMap<Long, FlashSale> sales = new ConcurrentHashMap<>();
    private long nextId = 1;
    
    public FlashSale createSale(Long productId, String productName, Long sellerId, String sellerName,
                                 double originalPrice, double flashPrice, int totalStock, int durationHours) {
        FlashSale sale = new FlashSale(nextId++, productId, productName, sellerId, sellerName,
                                        originalPrice, flashPrice, totalStock, durationHours);
        sales.put(sale.getId(), sale);
        return sale;
    }
    
    public List<FlashSale> getActiveSales() {
        return sales.values().stream()
                .filter(FlashSale::isActive)
                .collect(Collectors.toList());
    }
    
    public FlashSale getSale(Long saleId) {
        FlashSale sale = sales.get(saleId);
        if (sale != null && !sale.isActive()) {
            sale.setStatus("ENDED");
        }
        return sale;
    }
    
    public FlashSale recordSale(Long saleId) {
        FlashSale sale = sales.get(saleId);
        if (sale != null && sale.isActive()) {
            sale.setSoldCount(sale.getSoldCount() + 1);
            if (sale.getRemainingStock() <= 0) {
                sale.setStatus("ENDED");
            }
        }
        return sale;
    }
}