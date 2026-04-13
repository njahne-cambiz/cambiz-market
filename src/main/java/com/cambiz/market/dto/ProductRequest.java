package com.cambiz.market.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductRequest {
    private String name;
    private String description;
    private Double price;
    private Double discountedPrice;
    private Integer quantity;
    private String condition;
    private List<String> imageUrls;
    private Long categoryId;
    private Boolean isFeatured;
}