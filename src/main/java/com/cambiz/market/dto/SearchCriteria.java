package com.cambiz.market.dto;

import lombok.Data;

@Data
public class SearchCriteria {
    private String keyword;
    private Long categoryId;
    private Double minPrice;
    private Double maxPrice;
    private String location;
    private String productCondition;
    private Double minRating;
    private Boolean inStock;
    private Boolean freeDelivery;
    private Boolean isFeatured;
    private Boolean onDiscount;
    private String sortBy;
    private Integer page = 0;
    private Integer size = 20;
    
    public Integer getPage() {
        return page != null ? page : 0;
    }
    
    public Integer getSize() {
        return size != null ? size : 20;
    }
}