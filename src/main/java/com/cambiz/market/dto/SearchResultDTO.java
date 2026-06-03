package com.cambiz.market.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SearchResultDTO {
    private List<ProductCardDTO> products;
    private long totalResults;
    private int totalPages;
    private int currentPage;
    private Map<String, Long> categoryFacets;
    private Map<String, Long> locationFacets;
    private Map<String, Long> priceRangeFacets;
    private Map<String, Long> conditionFacets;
    private List<String> suggestedKeywords;
    private String didYouMean;
}