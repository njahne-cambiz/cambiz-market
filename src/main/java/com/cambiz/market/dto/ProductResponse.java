package com.cambiz.market.dto;

import com.cambiz.market.model.Product;
import com.cambiz.market.model.User;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Double discountedPrice;
    private Integer stockQuantity;
    private String condition;
    private List<String> imageUrls;
    private Long sellerId;
    private String sellerName;
    private String sellerBusinessName;
    private Long categoryId;
    private String categoryName;
    private Boolean isFeatured;
    private Integer viewCount;
    private LocalDateTime createdAt;
    
    // ========== PRODUCT APPROVAL FIELDS ==========
    private Boolean isApproved;
    private String status;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime rejectedAt;
    private Double minAcceptablePrice;
    private Boolean isActive;
    
    public static ProductResponse fromProduct(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setDiscountedPrice(product.getDiscountedPrice());
        response.setStockQuantity(product.getStockQuantity());
        response.setCondition(product.getProductCondition());
        response.setImageUrls(product.getImageUrls());
        
        // Handle seller info
        User seller = product.getSeller();
        if (seller != null) {
            response.setSellerId(seller.getId());
            StringBuilder nameBuilder = new StringBuilder();
            if (seller.getFirstName() != null && !seller.getFirstName().isEmpty()) {
                nameBuilder.append(seller.getFirstName());
            }
            if (seller.getLastName() != null && !seller.getLastName().isEmpty()) {
                if (nameBuilder.length() > 0) nameBuilder.append(" ");
                nameBuilder.append(seller.getLastName());
            }
            if (nameBuilder.length() == 0) {
                nameBuilder.append("Seller #").append(seller.getId());
            }
            response.setSellerName(nameBuilder.toString());
            response.setSellerBusinessName(seller.getBusinessName());
        } else {
            response.setSellerName("Unknown Seller");
        }
        
        // Handle category
        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getId());
            response.setCategoryName(product.getCategory().getNameEn());
        }
        
        response.setIsFeatured(product.getIsFeatured());
        response.setViewCount(product.getViewCount());
        response.setCreatedAt(product.getCreatedAt());
        
        // ========== APPROVAL FIELDS ==========
        response.setIsApproved(product.getIsApproved());
        response.setStatus(product.getStatus() != null ? product.getStatus().name() : "PENDING_APPROVAL");
        response.setApprovedBy(product.getApprovedBy());
        response.setApprovedAt(product.getApprovedAt());
        response.setRejectionReason(product.getRejectionReason());
        response.setRejectedAt(product.getRejectedAt());
        response.setMinAcceptablePrice(product.getMinAcceptablePrice());
        response.setIsActive(product.getIsActive());
        
        return response;
    }
    
    public static List<ProductResponse> fromProducts(List<Product> products) {
        if (products == null) {
            return List.of();
        }
        return products.stream()
                .map(ProductResponse::fromProduct)
                .collect(Collectors.toList());
    }
}