package com.cambiz.market.service;

import com.cambiz.market.dto.*;
import com.cambiz.market.model.*;
import com.cambiz.market.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MakolaService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PriceNegotiationRepository negotiationRepository;

    @Autowired
    private CartService cartService;

    private static final List<PriceNegotiation.NegotiationStatus> ACTIVE_STATUSES =
        Arrays.asList(PriceNegotiation.NegotiationStatus.PENDING,
                      PriceNegotiation.NegotiationStatus.COUNTERED);

    private static final List<PriceNegotiation.NegotiationStatus> ALL_STATUSES =
        Arrays.asList(PriceNegotiation.NegotiationStatus.PENDING,
                      PriceNegotiation.NegotiationStatus.COUNTERED,
                      PriceNegotiation.NegotiationStatus.ACCEPTED,
                      PriceNegotiation.NegotiationStatus.REJECTED);

    private static final double MINIMUM_PERCENTAGE = 0.30;
    private static final double FAIR_OFFER_THRESHOLD = 0.80;
    private static final double GOOD_OFFER_THRESHOLD = 0.90;
    private static final int NEGOTIATION_EXPIRY_DAYS = 7;

    @Transactional
    public MakolaResponse makeOffer(Long buyerId, MakolaOfferRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new RuntimeException("Product not found"));

        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new RuntimeException("Buyer not found"));

        Optional<PriceNegotiation> existing = negotiationRepository
            .findByBuyerAndProductIdAndStatusIn(buyer, product.getId(), ACTIVE_STATUSES);
        if (existing.isPresent()) {
            throw new RuntimeException("You already have an active negotiation for this product");
        }

        double originalPrice = product.getPrice() != null ? product.getPrice() : 0;
        double offerAmount = request.getOfferAmount().doubleValue();
        double minOffer = originalPrice * MINIMUM_PERCENTAGE;

        if (offerAmount < minOffer) {
            throw new RuntimeException("Offer cannot be less than 30% of original price (" + 
                String.format("%.0f", minOffer) + " XAF)");
        }

        double offerPercentage = offerAmount / originalPrice;
        String aiMessage;
        double aiSuggestedPrice;

        if (offerPercentage >= GOOD_OFFER_THRESHOLD) {
            aiSuggestedPrice = offerAmount;
            aiMessage = "Great offer! This is above the typical negotiated price. Seller will likely accept quickly!";
        } else if (offerPercentage >= FAIR_OFFER_THRESHOLD) {
            if (product.getStockQuantity() != null && product.getStockQuantity() > 20) {
                aiSuggestedPrice = originalPrice * 0.85;
                aiMessage = "Fair offer! Within range of typical negotiations. High stock levels - seller may be flexible!";
            } else {
                aiSuggestedPrice = originalPrice * 0.88;
                aiMessage = "Fair offer! Within range of typical negotiations.";
            }
        } else {
            double recommended = originalPrice * 0.85;
            aiSuggestedPrice = recommended;
            if (product.getStockQuantity() != null && product.getStockQuantity() > 20) {
                aiMessage = "This offer is below typical range. Consider raising to around " +
                    String.format("%.0f", recommended) + " XAF. High stock levels - seller may be flexible!";
            } else {
                aiMessage = "This offer is below typical range. Consider raising to around " +
                    String.format("%.0f", recommended) + " XAF.";
            }
        }

        PriceNegotiation negotiation = new PriceNegotiation();
        negotiation.setProduct(product);
        negotiation.setBuyer(buyer);
        negotiation.setOriginalPrice(BigDecimal.valueOf(originalPrice));
        negotiation.setBuyerOffer(BigDecimal.valueOf(offerAmount));
        negotiation.setAiSuggestedPrice(BigDecimal.valueOf(aiSuggestedPrice));
        negotiation.setAiReasoning(aiMessage);
        negotiation.setBuyerMessage(request.getMessage());
        negotiation.setStatus(PriceNegotiation.NegotiationStatus.PENDING);
        negotiation.setExpiresAt(LocalDateTime.now().plusDays(NEGOTIATION_EXPIRY_DAYS));

        negotiationRepository.save(negotiation);

        MakolaResponse response = MakolaResponse.fromNegotiation(negotiation);
        response.setAiMessage(aiMessage);
        response.setAiSuggestedPrice(BigDecimal.valueOf(aiSuggestedPrice));

        return response;
    }

    @Transactional
    public MakolaResponse counterOffer(Long sellerId, Long negotiationId, MakolaCounterRequest request) {
        PriceNegotiation negotiation = negotiationRepository.findById(negotiationId)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));

        if (!negotiation.getProduct().getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("You can only counter offers on your own products");
        }

        if (negotiation.getStatus() != PriceNegotiation.NegotiationStatus.PENDING) {
            throw new RuntimeException("This negotiation is no longer active");
        }

        double counterAmount = request.getCounterAmount().doubleValue();
        BigDecimal origPrice = negotiation.getOriginalPrice();
        double halfPrice = (origPrice != null ? origPrice.doubleValue() : 0) * 0.5;

        if (counterAmount < halfPrice) {
            throw new RuntimeException("Counter offer cannot be less than 50% of original price");
        }

        negotiation.setSellerCounter(BigDecimal.valueOf(counterAmount));
        negotiation.setSellerMessage(request.getMessage());
        negotiation.setStatus(PriceNegotiation.NegotiationStatus.COUNTERED);
        negotiationRepository.save(negotiation);

        return MakolaResponse.fromNegotiation(negotiation);
    }

    @Transactional
    public MakolaResponse acceptOffer(Long userId, Long negotiationId) {
        PriceNegotiation negotiation = negotiationRepository.findById(negotiationId)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));

        boolean isBuyer = negotiation.getBuyer().getId().equals(userId);
        boolean isSeller = negotiation.getProduct().getSeller().getId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new RuntimeException("You are not part of this negotiation");
        }

        BigDecimal finalPrice;
        if (isSeller) {
            finalPrice = negotiation.getBuyerOffer();
        } else {
            if (negotiation.getSellerCounter() == null) {
                throw new RuntimeException("No counter offer to accept");
            }
            finalPrice = negotiation.getSellerCounter();
        }

        negotiation.setFinalPrice(finalPrice);
        negotiation.setAcceptedAt(LocalDateTime.now());
        negotiation.setStatus(PriceNegotiation.NegotiationStatus.ACCEPTED);
        negotiationRepository.save(negotiation);

        return MakolaResponse.fromNegotiation(negotiation);
    }

    @Transactional
    public void rejectOffer(Long sellerId, Long negotiationId, String reason) {
        PriceNegotiation negotiation = negotiationRepository.findById(negotiationId)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));

        if (!negotiation.getProduct().getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("You can only reject offers on your own products");
        }

        negotiation.setStatus(PriceNegotiation.NegotiationStatus.REJECTED);
        if (reason != null && !reason.isEmpty()) {
            negotiation.setSellerMessage(reason);
        }
        negotiationRepository.save(negotiation);
    }

    @Transactional
    public CartResponse addNegotiatedToCart(Long buyerId, Long negotiationId, Integer quantity) {
        PriceNegotiation negotiation = negotiationRepository.findById(negotiationId)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));

        if (!negotiation.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("This is not your negotiation");
        }

        if (negotiation.getStatus() != PriceNegotiation.NegotiationStatus.ACCEPTED) {
            throw new RuntimeException("This negotiation hasn't been accepted yet");
        }

        double finalPrice = negotiation.getFinalPrice() != null ? 
            negotiation.getFinalPrice().doubleValue() : negotiation.getBuyerOffer().doubleValue();
        int qty = quantity != null ? quantity : 1;

        cartService.addToCartWithCustomPrice(buyerId, negotiation.getProduct().getId(), qty, finalPrice);

        negotiation.setStatus(PriceNegotiation.NegotiationStatus.CONVERTED_TO_CART);
        negotiationRepository.save(negotiation);

        return cartService.getCart(buyerId);
    }

    @Transactional(readOnly = true)
    public List<MakolaResponse> getBuyerActiveNegotiations(Long buyerId) {
        User buyer = userRepository.findById(buyerId)
            .orElseThrow(() -> new RuntimeException("Buyer not found"));

        List<PriceNegotiation> negotiations = negotiationRepository
            .findByBuyerAndStatusIn(buyer, ALL_STATUSES);

        negotiations.forEach(n -> {
            if (n.getProduct() != null) n.getProduct().getName();
            if (n.getProduct() != null && n.getProduct().getSeller() != null) n.getProduct().getSeller().getEmail();
        });

        return negotiations.stream()
            .map(MakolaResponse::fromNegotiation)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MakolaResponse> getSellerActiveNegotiations(Long sellerId) {
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new RuntimeException("Seller not found"));

        List<PriceNegotiation> negotiations = negotiationRepository
            .findByProductSellerAndStatusIn(seller, ACTIVE_STATUSES);

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

    @Transactional(readOnly = true)
    public MakolaResponse getNegotiation(Long negotiationId, Long userId) {
        PriceNegotiation negotiation = negotiationRepository.findById(negotiationId)
            .orElseThrow(() -> new RuntimeException("Negotiation not found"));

        return MakolaResponse.fromNegotiation(negotiation);
    }
}