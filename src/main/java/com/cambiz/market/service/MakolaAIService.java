package com.cambiz.market.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.cambiz.market.model.Product;
import com.cambiz.market.repository.PriceNegotiationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MakolaAIService {
    
    private final PriceNegotiationRepository negotiationRepository;
    
    /**
     * AI calculates fair price based on multiple factors
     */
    public FairPriceAnalysis analyzeFairPrice(Product product, BigDecimal buyerOffer) {
        BigDecimal originalPrice = product.getPrice() != null ? 
            BigDecimal.valueOf(product.getPrice()) : BigDecimal.ZERO;
        
        if (originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return new FairPriceAnalysis(originalPrice, "No price data available");
        }
        
        // Factor 1: Historical negotiations for this product
        Double avgAccepted = negotiationRepository.getAverageAcceptedPercentage(product.getId());
        BigDecimal historicalFactor = avgAccepted != null ? 
            BigDecimal.valueOf(avgAccepted) : new BigDecimal("0.85");
        
        // Factor 2: Category average discount
        Double categoryAvg = product.getCategory() != null ? 
            negotiationRepository.getCategoryAverageDiscount(product.getCategory().getId()) : null;
        BigDecimal categoryFactor = categoryAvg != null ? 
            BigDecimal.valueOf(categoryAvg) : new BigDecimal("0.90");
        
        // Factor 3: Stock level (high stock = more discount possible)
        BigDecimal stockFactor = calculateStockFactor(product);
        
        // Factor 4: Product age (older products = more discount)
        BigDecimal ageFactor = calculateAgeFactor(product);
        
        // Weighted calculation
        BigDecimal fairPrice = originalPrice
            .multiply(historicalFactor.multiply(new BigDecimal("0.4")))
            .add(originalPrice.multiply(categoryFactor.multiply(new BigDecimal("0.3"))))
            .add(originalPrice.multiply(stockFactor.multiply(new BigDecimal("0.2"))))
            .add(originalPrice.multiply(ageFactor.multiply(new BigDecimal("0.1"))));
        
        fairPrice = fairPrice.setScale(0, RoundingMode.HALF_UP);
        
        // Generate AI reasoning message
        String reasoning = generateReasoning(buyerOffer, fairPrice, originalPrice, 
            historicalFactor, stockFactor, ageFactor);
        
        return new FairPriceAnalysis(fairPrice, reasoning);
    }
    
    private BigDecimal calculateStockFactor(Product product) {
        Integer stock = product.getStockQuantity();
        if (stock == null) return new BigDecimal("0.95");
        if (stock > 50) return new BigDecimal("0.85"); // High stock, more discount
        if (stock > 20) return new BigDecimal("0.90");
        if (stock > 5) return new BigDecimal("0.95");
        return new BigDecimal("0.98"); // Low stock, less discount
    }
    
    private BigDecimal calculateAgeFactor(Product product) {
        if (product.getCreatedAt() == null) return new BigDecimal("0.95");
        
        long daysOld = java.time.Duration.between(
            product.getCreatedAt(), 
            java.time.LocalDateTime.now()
        ).toDays();
        
        if (daysOld > 90) return new BigDecimal("0.80"); // Old product, more discount
        if (daysOld > 30) return new BigDecimal("0.85");
        if (daysOld > 7) return new BigDecimal("0.90");
        return new BigDecimal("0.95"); // New product, less discount
    }
    
    private String generateReasoning(BigDecimal offer, BigDecimal fairPrice, 
                                     BigDecimal original, BigDecimal hist, 
                                     BigDecimal stock, BigDecimal age) {
        double offerPercent = offer.divide(original, 2, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100")).doubleValue();
        double fairPercent = fairPrice.divide(original, 2, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100")).doubleValue();
        
        StringBuilder sb = new StringBuilder();
        
        if (offer.compareTo(fairPrice) >= 0) {
            sb.append("🎉 Great offer! This is above the typical negotiated price. ");
            sb.append("Seller will likely accept quickly! ");
        } else if (offerPercent >= fairPercent - 10) {
            sb.append("👍 Fair offer! Within range of typical negotiations. ");
            sb.append("Seller may counter, but you're close! ");
        } else if (offerPercent >= 50) {
            sb.append("💭 This offer is below typical range. ");
            sb.append("Consider raising to around ").append(fairPrice).append(" XAF ");
            sb.append("for better chances. ");
        } else {
            sb.append("⚠️ This offer is very low. ");
            sb.append("Sellers rarely accept below 50% of asking price. ");
            sb.append("Try ").append(fairPrice).append(" XAF instead. ");
        }
        
        // Add market insights
        if (stock.doubleValue() < 0.90) {
            sb.append("📊 High stock levels - seller may be flexible! ");
        }
        
        if (hist.doubleValue() < 0.85) {
            sb.append("📈 This product often sells at a discount. ");
        }
        
        return sb.toString();
    }
    
    // Inner class for AI analysis result
    public static class FairPriceAnalysis {
        private final BigDecimal suggestedPrice;
        private final String reasoning;
        
        public FairPriceAnalysis(BigDecimal suggestedPrice, String reasoning) {
            this.suggestedPrice = suggestedPrice;
            this.reasoning = reasoning;
        }
        
        public BigDecimal getSuggestedPrice() { return suggestedPrice; }
        public String getReasoning() { return reasoning; }
    }
}