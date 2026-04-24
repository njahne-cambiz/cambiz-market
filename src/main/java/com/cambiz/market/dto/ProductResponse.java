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
    private Long categoryId;        // ✅ ADDED
    private String categoryName;
    private Boolean isFeatured;
    private Integer viewCount;
    private LocalDateTime createdAt;
    
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
        
        // Handle seller info safely
        User seller = product.getSeller();
        if (seller != null) {
            response.setSellerId(seller.getId());
            String sellerName = "Seller";
            try {
                sellerName = seller.getFirstName() + " " + seller.getLastName();
            } catch (Exception e) {
                sellerName = "Seller #" + seller.getId();
            }
            response.setSellerName(sellerName);
            response.setSellerBusinessName(seller.getBusinessName());
        }
        
        // Handle category - SET BOTH ID AND NAME
        if (product.getCategory() != null) {
            response.setCategoryId(product.getCategory().getId());       // ✅ ADDED
            response.setCategoryName(product.getCategory().getNameEn());
        }
        
        response.setIsFeatured(product.getIsFeatured());
        response.setViewCount(product.getViewCount());
        response.setCreatedAt(product.getCreatedAt());
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