package com.cambiz.market.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false)
    private Double price;
    
    private Double discountedPrice;
    
    private Integer stockQuantity = 1;
    
    @Column(name = "product_condition")
    private String productCondition;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    
    private Boolean isFeatured = false;
    
    @Column(name = "featured_until")
    private LocalDateTime featuredUntil;          // ✅ ADDED
    
    @Column(name = "featured_payment_id")
    private Long featuredPaymentId;               // ✅ ADDED
    
    private Boolean isActive = true;
    
    private Integer viewCount = 0;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public Long getSellerId() {
        return seller != null ? seller.getId() : null;
    }
    
    public String getImageUrl() {
        return imageUrls != null && !imageUrls.isEmpty() ? imageUrls.get(0) : null;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}