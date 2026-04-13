package com.cambiz.market.service;

import com.cambiz.market.dto.*;
import com.cambiz.market.model.*;
import com.cambiz.market.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional // ✅ ADDED AT CLASS LEVEL
public class MakolaService {
    
    private final PriceNegotiationRepository negotiationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final MakolaAIService aiService;
    private final CartService cartService;
    
    private static final List<PriceNegotiation.NegotiationStatus> ACTIVE_STATUSES = 
        Arrays.asList(PriceNegotiation.NegotiationStatus.PENDING, 
                      PriceNegotiation.NegotiationStatus.COUNTERED);
    
    /**
     * BUYER: Make an offer on a product
     */
    public MakolaResponse makeOffer(Long buyerId, MakolaOfferRequest request) {
        log.info("Buyer {} making offer on product {}", buyerId, request.getProductId());
        
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RuntimeException("Product not found"));
        
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new RuntimeException("Buyer not found"));
        
        // Check if buyer already has active negotiation for this product
        negotiationRepository.findByBuyerAndProductIdAndStatusIn(buyer, product.getId(), ACTIVE_STATUSES)
            .ifPresent(n -> {
                throw new RuntimeException("You already have an active negotiation for this product");
            });
        
        // Validate offer amount
        BigDecimal originalPrice = BigDecimal.valueOf(product.getPrice());
        BigDecimal minOffer = originalPrice.multiply(new BigDecimal("0.3"));
        
        if (request.getOfferAmount().compareTo(minOffer) < 0) {
            throw new RuntimeException("Offer cannot be less than 30% of original price");
        }
        
        if (request.getOfferAmount().compareTo(originalPrice) > 0) {
            throw new RuntimeException("Offer cannot be higher than original price");
        }
        
        // AI Analysis
        MakolaAIService.FairPriceAnalysis aiAnalysis = aiService.analyzeFairPrice(product, request.getOfferAmount());
        
        // Create negotiation
        PriceNegotiation negotiation = new PriceNegotiation();
        negotiation.setProduct(product);
        negotiation.setBuyer(buyer);
        negotiation.setOriginalPrice(originalPrice);
        negotiation.setBuyerOffer(request.getOfferAmount());
        negotiation.setBuyerMessage(request.getMessage());
        negotiation.setStatus(PriceNegotiation.NegotiationStatus.PENDING);
        negotiation.setAiSuggestedPrice(aiAnalysis.getSuggestedPrice());
        negotiation.setAiReasoning(aiAnalysis.getReasoning());
        
        PriceNegotiation saved = negotiationRepository.save(negotiation);
        
        log.info("Negotiation created: ID {} with AI suggestion {}", saved.getId(), aiAnalysis.getSuggestedPrice());
        
        return MakolaResponse.fromNegotiation(saved);
    }
    
    /**
     * SELLER: Counter offer
     */
    public MakolaResponse counterOffer(Long sellerId, Long negotiationId, MakolaCounterRequest request) {
        PriceNegotiation negotiation = negotiationRepository.findById(negotiationId)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));
        
        // Verify seller owns the product
        if (!negotiation.getProduct().getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Unauthorized: You don't own this product");
        }
        
        // Validate counter amount
        if (request.getCounterAmount().compareTo(negotiation.getBuyerOffer()) < 0) {
            throw new RuntimeException("Counter offer cannot be lower than buyer's offer");
        }
        
        if (request.getCounterAmount().compareTo(negotiation.getOriginalPrice()) > 0) {
            throw new RuntimeException("Counter offer cannot be higher than original price");
        }
        
        negotiation.setSellerCounter(request.getCounterAmount());
        negotiation.setSellerMessage(request.getMessage());
        negotiation.setStatus(PriceNegotiation.NegotiationStatus.COUNTERED);
        negotiation.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        PriceNegotiation saved = negotiationRepository.save(negotiation);
        
        log.info("Seller countered on negotiation {}: {}", negotiationId, request.getCounterAmount());
        
        return MakolaResponse.fromNegotiation(saved);
    }
    
    /**
     * BUYER or SELLER: Accept the current offer
     */
    public MakolaResponse acceptOffer(Long userId, Long negotiationId) {
        PriceNegotiation negotiation = negotiationRepository.findById(negotiationId)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));
        
        BigDecimal finalPrice;
        
        // Determine who is accepting and what price
        if (negotiation.getBuyer().getId().equals(userId)) {
            // Buyer accepting seller's counter
            if (negotiation.getStatus() != PriceNegotiation.NegotiationStatus.COUNTERED) {
                throw new RuntimeException("No counter offer to accept");
            }
            finalPrice = negotiation.getSellerCounter();
            log.info("Buyer accepted seller's counter: {}", finalPrice);
        } else if (negotiation.getProduct().getSeller().getId().equals(userId)) {
            // Seller accepting buyer's offer
            if (negotiation.getStatus() != PriceNegotiation.NegotiationStatus.PENDING) {
                throw new RuntimeException("No offer to accept");
            }
            finalPrice = negotiation.getBuyerOffer();
            log.info("Seller accepted buyer's offer: {}", finalPrice);
        } else {
            throw new RuntimeException("Unauthorized");
        }
        
        negotiation.setFinalPrice(finalPrice);
        negotiation.setStatus(PriceNegotiation.NegotiationStatus.ACCEPTED);
        negotiation.setAcceptedAt(LocalDateTime.now());
        
        PriceNegotiation saved = negotiationRepository.save(negotiation);
        
        return MakolaResponse.fromNegotiation(saved);
    }
    
    /**
     * SELLER: Reject offer
     */
    public void rejectOffer(Long sellerId, Long negotiationId, String reason) {
        PriceNegotiation negotiation = negotiationRepository.findById(negotiationId)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));
        
        if (!negotiation.getProduct().getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        negotiation.setStatus(PriceNegotiation.NegotiationStatus.REJECTED);
        negotiation.setSellerMessage(reason);
        negotiationRepository.save(negotiation);
        
        log.info("Seller rejected negotiation {}", negotiationId);
    }
    
    /**
     * Add negotiated price to cart
     */
    public CartResponse addNegotiatedToCart(Long buyerId, Long negotiationId, Integer quantity) {
        PriceNegotiation negotiation = negotiationRepository.findById(negotiationId)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));
        
        if (!negotiation.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (negotiation.getStatus() != PriceNegotiation.NegotiationStatus.ACCEPTED) {
            throw new RuntimeException("Negotiation must be accepted first");
        }
        
        if (negotiation.getFinalPrice() == null) {
            throw new RuntimeException("No final price agreed");
        }
        
        CartResponse cart = cartService.addToCartWithCustomPrice(
            buyerId, 
            negotiation.getProduct().getId(), 
            quantity, 
            negotiation.getFinalPrice().doubleValue()
        );
        
        // Mark negotiation as converted
        negotiation.setStatus(PriceNegotiation.NegotiationStatus.CONVERTED_TO_CART);
        negotiationRepository.save(negotiation);
        
        log.info("Negotiated price added to cart: {} XAF", negotiation.getFinalPrice());
        
        return cart;
    }
    
    /**
     * Get buyer's active negotiations - FIXED with @Transactional
     */
    @Transactional(readOnly = true) // ✅ ADDED
    public List<MakolaResponse> getBuyerActiveNegotiations(Long buyerId) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new RuntimeException("Buyer not found"));
        
        List<PriceNegotiation> negotiations = negotiationRepository
            .findByBuyerAndStatusIn(buyer, ACTIVE_STATUSES);
        
        // ✅ FIXED: Force initialization of lazy collections
        negotiations.forEach(n -> {
            if (n.getProduct() != null) {
                n.getProduct().getName(); // Force product load
                n.getProduct().getPrice(); // Force price load
                if (n.getProduct().getSeller() != null) {
                    n.getProduct().getSeller().getId(); // Force seller load
                }
            }
            if (n.getBuyer() != null) {
                n.getBuyer().getEmail(); // Force buyer load
            }
        });
        
        return negotiations.stream()
            .map(MakolaResponse::fromNegotiation)
            .collect(Collectors.toList());
    }
    
    /**
     * Get seller's active negotiations - FIXED with @Transactional
     */
    @Transactional(readOnly = true) // ✅ ADDED
    public List<MakolaResponse> getSellerActiveNegotiations(Long sellerId) {
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        List<PriceNegotiation> negotiations = negotiationRepository
            .findByProductSellerAndStatusIn(seller, ACTIVE_STATUSES);
        
        // ✅ FIXED: Force initialization of lazy collections
        negotiations.forEach(n -> {
            if (n.getProduct() != null) {
                n.getProduct().getName();
                n.getProduct().getPrice();
            }
            if (n.getBuyer() != null) {
                n.getBuyer().getEmail();
                n.getBuyer().getFirstName();
            }
        });
        
        return negotiations.stream()
            .map(MakolaResponse::fromNegotiation)
            .collect(Collectors.toList());
    }
    
    /**
     * Get single negotiation details - FIXED with @Transactional
     */
    @Transactional(readOnly = true) // ✅ ADDED
    public MakolaResponse getNegotiation(Long negotiationId, Long userId) {
        PriceNegotiation negotiation = negotiationRepository.findById(negotiationId)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));
        
        // Force initialization of lazy collections
        if (negotiation.getProduct() != null) {
            negotiation.getProduct().getName();
            negotiation.getProduct().getPrice();
            if (negotiation.getProduct().getSeller() != null) {
                negotiation.getProduct().getSeller().getId();
            }
        }
        if (negotiation.getBuyer() != null) {
            negotiation.getBuyer().getEmail();
        }
        
        // Security: Only buyer or seller can view
        boolean isBuyer = negotiation.getBuyer().getId().equals(userId);
        boolean isSeller = negotiation.getProduct().getSeller().getId().equals(userId);
        
        if (!isBuyer && !isSeller) {
            throw new RuntimeException("Unauthorized");
        }
        
        return MakolaResponse.fromNegotiation(negotiation);
    }
    
    /**
     * Get all negotiations (Admin only)
     */
    @Transactional(readOnly = true)
    public List<MakolaResponse> getAllNegotiations() {
        List<PriceNegotiation> negotiations = negotiationRepository.findAll();
        
        negotiations.forEach(n -> {
            if (n.getProduct() != null) n.getProduct().getName();
            if (n.getBuyer() != null) n.getBuyer().getEmail();
        });
        
        return negotiations.stream()
            .map(MakolaResponse::fromNegotiation)
            .collect(Collectors.toList());
    }
}