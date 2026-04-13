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
            
            // Build seller name safely using reflection
            String sellerName = "Seller";
            try {
                // Try to get firstName and lastName
                java.lang.reflect.Method getFirstName = seller.getClass().getMethod("getFirstName");
                java.lang.reflect.Method getLastName = seller.getClass().getMethod("getLastName");
                
                String firstName = (String) getFirstName.invoke(seller);
                String lastName = (String) getLastName.invoke(seller);
                
                if (firstName != null && lastName != null) {
                    sellerName = firstName + " " + lastName;
                } else if (firstName != null) {
                    sellerName = firstName;
                } else if (lastName != null) {
                    sellerName = lastName;
                }
            } catch (Exception e) {
                // Methods don't exist, try other options
                try {
                    java.lang.reflect.Method getName = seller.getClass().getMethod("getName");
                    sellerName = (String) getName.invoke(seller);
                } catch (Exception e2) {
                    try {
                        java.lang.reflect.Method getFullName = seller.getClass().getMethod("getFullName");
                        sellerName = (String) getFullName.invoke(seller);
                    } catch (Exception e3) {
                        sellerName = "Seller #" + seller.getId();
                    }
                }
            }
            response.setSellerName(sellerName);
            
            // FIXED LINE 77: Safely get business name or set to null
            try {
                java.lang.reflect.Method getBusinessName = seller.getClass().getMethod("getBusinessName");
                String businessName = (String) getBusinessName.invoke(seller);
                response.setSellerBusinessName(businessName);
            } catch (Exception e) {
                // Business name method doesn't exist, set to null or use seller name
                response.setSellerBusinessName(null);
            }
        }
        
        // Handle category safely
        if (product.getCategory() != null) {
            try {
                java.lang.reflect.Method getNameEn = product.getCategory().getClass().getMethod("getNameEn");
                response.setCategoryName((String) getNameEn.invoke(product.getCategory()));
            } catch (Exception e) {
                try {
                    java.lang.reflect.Method getName = product.getCategory().getClass().getMethod("getName");
                    response.setCategoryName((String) getName.invoke(product.getCategory()));
                } catch (Exception e2) {
                    response.setCategoryName("Category #" + product.getCategory().getId());
                }
            }
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