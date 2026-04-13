package com.cambiz.market.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private List<CartItem> items = new ArrayList<>();
    private Double subtotal = 0.0;
    private Double deliveryFee = 0.0;
    private Double total = 0.0;
    private Integer itemCount = 0;
    private Integer totalQuantity = 0;
    private Map<Long, List<CartItem>> itemsBySeller;
    private List<String> warnings = new ArrayList<>();
    
    public void calculateTotals() {
        if (items == null) {
            items = new ArrayList<>();
        }
        
        // Calculate subtotal
        this.subtotal = items.stream()
            .mapToDouble(item -> item.getSubtotal() != null ? item.getSubtotal() : 0.0)
            .sum();
        
        // Calculate total with delivery
        this.total = this.subtotal + (this.deliveryFee != null ? this.deliveryFee : 0.0);
        
        // Count items
        this.itemCount = items.size();
        
        // Calculate total quantity
        this.totalQuantity = items.stream()
            .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
            .sum();
        
        // Group items by seller (for multi-seller checkout)
        this.itemsBySeller = items.stream()
            .filter(item -> item.getSellerId() != null)
            .collect(Collectors.groupingBy(CartItem::getSellerId));
    }
    
    // Helper method to add a warning
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }
    
    // Helper method to check if cart has warnings
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    // Helper method to get items for a specific seller
    public List<CartItem> getItemsBySeller(Long sellerId) {
        if (itemsBySeller == null || sellerId == null) {
            return new ArrayList<>();
        }
        return itemsBySeller.getOrDefault(sellerId, new ArrayList<>());
    }
}