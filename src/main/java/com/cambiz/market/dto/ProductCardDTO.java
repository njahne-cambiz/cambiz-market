package com.cambiz.market.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCardDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Double discountedPrice;
    private String imageUrl;
    private String categoryName;
    private String categoryEmoji;
    private String sellerName;
    private String sellerLocation;
    private Double rating;
    private Integer reviewCount;
    private Integer soldCount;
    private Boolean inStock;
    private Boolean isFeatured;
    private Boolean freeDelivery;
    private String productCondition;
}