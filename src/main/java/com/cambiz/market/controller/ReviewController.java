package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.model.Product;
import com.cambiz.market.model.Review;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.ReviewRepository;
import com.cambiz.market.repository.UserRepository;
import com.cambiz.market.security.JwtUtils;
import com.cambiz.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse> addReview(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = jwtUtils.extractUsername(token);
            Long userId = userService.getUserIdByEmail(email);
            
            Long productId = Long.valueOf(request.get("productId").toString());
            int rating = Integer.parseInt(request.get("rating").toString());
            String comment = request.getOrDefault("comment", "").toString();

            Product product = productRepository.findById(productId).orElse(null);
            User user = userRepository.findById(userId).orElse(null);

            if (product == null) return ResponseEntity.badRequest().body(new ApiResponse(false, "Product not found", null));
            if (user == null) return ResponseEntity.badRequest().body(new ApiResponse(false, "User not found", null));
            if (rating < 1 || rating > 5) return ResponseEntity.badRequest().body(new ApiResponse(false, "Rating must be 1-5", null));

            Review review = new Review();
            review.setProduct(product);
            review.setUser(user);
            review.setRating(rating);
            review.setComment(comment);
            reviewRepository.save(review);

            return ResponseEntity.ok(new ApiResponse(true, "Review added!", getProductStats(productId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    @GetMapping("/product/{productId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getProductReviews(@PathVariable Long productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Review r : reviews) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("rating", r.getRating());
            map.put("comment", r.getComment());
            map.put("userName", "User #" + r.getUser().getId());
            map.put("createdAt", r.getCreatedAt());
            result.add(map);
        }
        return ResponseEntity.ok(new ApiResponse(true, "Reviews retrieved", Map.of(
            "reviews", result,
            "stats", getProductStats(productId)
        )));
    }

    private Map<String, Object> getProductStats(Long productId) {
        long count = reviewRepository.countByProductId(productId);
        double avg = 0;
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
        if (!reviews.isEmpty()) {
            avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        }
        return Map.of("totalReviews", count, "averageRating", Math.round(avg * 10) / 10.0);
    }
}