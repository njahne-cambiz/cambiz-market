package com.cambiz.market.controller;

import com.cambiz.market.dto.*;
import com.cambiz.market.service.MakolaService;
import com.cambiz.market.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/makola")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MakolaController {
    
    private final MakolaService makolaService;
    private final UserService userService;
    
    /**
     * BUYER: Make an offer
     * POST /api/makola/offer
     */
    @PostMapping("/offer")
    public ResponseEntity<ApiResponse> makeOffer(@Valid @RequestBody MakolaOfferRequest request) {
        try {
            Long buyerId = getCurrentUserId();
            MakolaResponse response = makolaService.makeOffer(buyerId, request);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Offer submitted successfully! " + response.getAiMessage(),
                response
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * SELLER: Counter offer
     * POST /api/makola/counter/{negotiationId}
     */
    @PostMapping("/counter/{negotiationId}")
    public ResponseEntity<ApiResponse> counterOffer(
            @PathVariable Long negotiationId,
            @Valid @RequestBody MakolaCounterRequest request) {
        try {
            Long sellerId = getCurrentUserId();
            MakolaResponse response = makolaService.counterOffer(sellerId, negotiationId, request);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Counter offer sent!",
                response
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * Accept offer (Buyer or Seller)
     * POST /api/makola/accept/{negotiationId}
     */
    @PostMapping("/accept/{negotiationId}")
    public ResponseEntity<ApiResponse> acceptOffer(@PathVariable Long negotiationId) {
        try {
            Long userId = getCurrentUserId();
            MakolaResponse response = makolaService.acceptOffer(userId, negotiationId);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "🎉 Deal! Price agreed at " + response.getFinalPrice() + " XAF!",
                response
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * SELLER: Reject offer
     * POST /api/makola/reject/{negotiationId}
     */
    @PostMapping("/reject/{negotiationId}")
    public ResponseEntity<ApiResponse> rejectOffer(
            @PathVariable Long negotiationId,
            @RequestParam(required = false) String reason) {
        try {
            Long sellerId = getCurrentUserId();
            makolaService.rejectOffer(sellerId, negotiationId, reason);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Offer rejected",
                null
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * Add negotiated price to cart
     * POST /api/makola/add-to-cart/{negotiationId}
     */
    @PostMapping("/add-to-cart/{negotiationId}")
    public ResponseEntity<ApiResponse> addToCart(
            @PathVariable Long negotiationId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            Long buyerId = getCurrentUserId();
            CartResponse cart = makolaService.addNegotiatedToCart(buyerId, negotiationId, quantity);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Added to cart with negotiated price! You saved! 💰",
                cart
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get buyer's active negotiations
     * GET /api/makola/my-offers
     */
    @GetMapping("/my-offers")
    public ResponseEntity<ApiResponse> getMyOffers() {
        try {
            Long buyerId = getCurrentUserId();
            List<MakolaResponse> offers = makolaService.getBuyerActiveNegotiations(buyerId);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Your active negotiations",
                offers
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get seller's active negotiations
     * GET /api/makola/seller/offers
     */
    @GetMapping("/seller/offers")
    public ResponseEntity<ApiResponse> getSellerOffers() {
        try {
            Long sellerId = getCurrentUserId();
            List<MakolaResponse> offers = makolaService.getSellerActiveNegotiations(sellerId);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Offers on your products",
                offers
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    /**
     * Get single negotiation details
     * GET /api/makola/{negotiationId}
     */
    @GetMapping("/{negotiationId}")
    public ResponseEntity<ApiResponse> getNegotiation(@PathVariable Long negotiationId) {
        try {
            Long userId = getCurrentUserId();
            MakolaResponse response = makolaService.getNegotiation(negotiationId, userId);
            
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Negotiation details",
                response
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, e.getMessage(), null));
        }
    }
    
    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userService.getUserIdByEmail(email);
        }
        throw new RuntimeException("User not authenticated");
    }
}