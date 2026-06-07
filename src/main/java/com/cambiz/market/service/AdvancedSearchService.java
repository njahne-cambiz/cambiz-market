package com.cambiz.market.service;

import com.cambiz.market.dto.ProductCardDTO;
import com.cambiz.market.dto.SearchCriteria;
import com.cambiz.market.dto.SearchResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AdvancedSearchService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final int MAX_SUGGESTIONS = 5;
    
    public SearchResultDTO search(SearchCriteria criteria) {
        SearchResultDTO result = new SearchResultDTO();
        
        String keyword = criteria.getKeyword() != null ? criteria.getKeyword().trim() : "";
        
        StringBuilder whereClause = new StringBuilder("WHERE p.is_approved = true AND p.is_active = true ");
        List<Object> params = new ArrayList<>();
        
        if (!keyword.isEmpty()) {
            whereClause.append("AND LOWER(p.name) LIKE LOWER(?) ");
            params.add("%" + keyword + "%");
            result.setDidYouMean(getSpellingSuggestion(keyword));
        }
        
        if (criteria.getCategoryId() != null) {
            whereClause.append("AND p.category_id = ? ");
            params.add(criteria.getCategoryId());
        }
        
        if (criteria.getMinPrice() != null) {
            whereClause.append("AND COALESCE(p.discounted_price, p.price) >= ? ");
            params.add(criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            whereClause.append("AND COALESCE(p.discounted_price, p.price) <= ? ");
            params.add(criteria.getMaxPrice());
        }
        
        if (criteria.getLocation() != null && !criteria.getLocation().isEmpty()) {
            whereClause.append("AND LOWER(u.city) LIKE LOWER(?) ");
            params.add("%" + criteria.getLocation() + "%");
        }
        
        if (criteria.getProductCondition() != null && !criteria.getProductCondition().isEmpty()) {
            whereClause.append("AND p.product_condition = ? ");
            params.add(criteria.getProductCondition());
        }
        
        if (criteria.getInStock() != null && criteria.getInStock()) {
            whereClause.append("AND p.stock_quantity > 0 ");
        }
        
        if (criteria.getIsFeatured() != null && criteria.getIsFeatured()) {
            whereClause.append("AND p.is_featured = true ");
        }
        
        if (criteria.getOnDiscount() != null && criteria.getOnDiscount()) {
            whereClause.append("AND p.discounted_price IS NOT NULL ");
        }
        
        if (criteria.getFreeDelivery() != null && criteria.getFreeDelivery()) {
            whereClause.append("AND p.free_delivery = true ");
        }
        
        // Count query
        String countSql = "SELECT COUNT(*) FROM products p LEFT JOIN users u ON p.seller_id = u.id " + whereClause.toString();
        long totalResults = 0;
        try {
            totalResults = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        } catch (Exception e) {
            totalResults = 0;
        }
        
        // Sort
        String orderBy = "ORDER BY p.is_featured DESC, p.created_at DESC";
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "relevance";
        switch (sortBy) {
            case "price_asc": orderBy = "ORDER BY COALESCE(p.discounted_price, p.price) ASC"; break;
            case "price_desc": orderBy = "ORDER BY COALESCE(p.discounted_price, p.price) DESC"; break;
            case "rating": orderBy = "ORDER BY avg_rating DESC"; break;
            case "newest": orderBy = "ORDER BY p.created_at DESC"; break;
            case "popular": orderBy = "ORDER BY p.view_count DESC"; break;
        }
        
        // Pagination
        int page = criteria.getPage();
        int size = criteria.getSize();
        
        // Main query
        String sql = "SELECT p.id as p_id, p.name as p_name, p.description as p_description, p.price as p_price, " +
                     "p.discounted_price as p_discounted_price, " +
                     "p.product_condition as p_product_condition, p.is_featured as p_is_featured, " +
                     "p.stock_quantity as p_stock_quantity, p.free_delivery as p_free_delivery, " +
                     "c.name as c_name, c.emoji as c_emoji, " +
                     "u.full_name as u_full_name, u.city as u_city, " +
                     "COALESCE(AVG(r.rating), 0) as avg_rating, COUNT(DISTINCT r.id) as review_count " +
                     "FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "LEFT JOIN users u ON p.seller_id = u.id " +
                     "LEFT JOIN reviews r ON p.id = r.product_id " +
                     whereClause.toString() +
                     " GROUP BY p.id, c.name, c.emoji, u.full_name, u.city " +
                     orderBy + " LIMIT ? OFFSET ?";
        
        params.add(size);
        params.add(page * size);
        
        List<ProductCardDTO> products = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
            for (Map<String, Object> row : rows) {
                ProductCardDTO dto = ProductCardDTO.builder()
                    .id(getLong(row, "p_id"))
                    .name(getString(row, "p_name"))
                    .description(getString(row, "p_description"))
                    .price(getDouble(row, "p_price"))
                    .discountedPrice(getDouble(row, "p_discounted_price"))
                    .productCondition(getString(row, "p_product_condition"))
                    .isFeatured(getBoolean(row, "p_is_featured"))
                    .inStock(getInt(row, "p_stock_quantity") > 0)
                    .freeDelivery(getBoolean(row, "p_free_delivery"))
                    .categoryName(getString(row, "c_name"))
                    .categoryEmoji(getString(row, "c_emoji"))
                    .sellerName(getString(row, "u_full_name"))
                    .sellerLocation(getString(row, "u_city"))
                    .rating(getDouble(row, "avg_rating"))
                    .reviewCount(getInt(row, "review_count"))
                    .imageUrl(null)
                    .build();
                products.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        result.setProducts(products);
        result.setTotalResults(totalResults);
        result.setTotalPages((int) Math.ceil((double) totalResults / size));
        result.setCurrentPage(page);
        result.setCategoryFacets(getCategoryFacets());
        result.setLocationFacets(getLocationFacets());
        result.setPriceRangeFacets(getPriceRangeFacets());
        result.setConditionFacets(getConditionFacets());
        
        if (!keyword.isEmpty()) {
            result.setSuggestedKeywords(getSearchSuggestions(keyword));
        }
        
        return result;
    }
    
    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
    
    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }
    
    private Double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }
    
    private Integer getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }
    
    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return false;
    }
    
    private Map<String, Long> getCategoryFacets() {
        String sql = "SELECT c.name, COUNT(p.id) FROM products p JOIN categories c ON p.category_id = c.id WHERE p.is_approved = true AND p.is_active = true GROUP BY c.name ORDER BY COUNT(p.id) DESC LIMIT 10";
        Map<String, Long> facets = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            try { facets.put(rs.getString(1), rs.getLong(2)); } catch (Exception e) {}
        });
        return facets;
    }
    
    private Map<String, Long> getLocationFacets() {
        String sql = "SELECT u.city, COUNT(p.id) FROM products p JOIN users u ON p.seller_id = u.id WHERE p.is_approved = true AND u.city IS NOT NULL GROUP BY u.city ORDER BY COUNT(p.id) DESC LIMIT 10";
        Map<String, Long> facets = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            try { facets.put(rs.getString(1), rs.getLong(2)); } catch (Exception e) {}
        });
        return facets;
    }
    
    private Map<String, Long> getPriceRangeFacets() {
        Map<String, Long> facets = new LinkedHashMap<>();
        int[][] ranges = {{0, 5000}, {5001, 10000}, {10001, 25000}, {25001, 50000}, {50001, 100000}, {100001, Integer.MAX_VALUE}};
        String[] labels = {"Under 5k", "5k-10k", "10k-25k", "25k-50k", "50k-100k", "100k+"};
        for (int i = 0; i < ranges.length; i++) {
            try {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products WHERE is_approved=true AND is_active=true AND COALESCE(discounted_price, price) BETWEEN ? AND ?", Long.class, ranges[i][0], ranges[i][1]);
                if (count != null && count > 0) { facets.put(labels[i], count); }
            } catch (Exception e) {}
        }
        return facets;
    }
    
    private Map<String, Long> getConditionFacets() {
        String sql = "SELECT product_condition, COUNT(id) FROM products WHERE is_approved = true AND is_active = true AND product_condition IS NOT NULL GROUP BY product_condition";
        Map<String, Long> facets = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            try { facets.put(rs.getString(1), rs.getLong(2)); } catch (Exception e) {}
        });
        return facets;
    }
    
    private List<String> getSearchSuggestions(String keyword) {
        try {
            return jdbcTemplate.queryForList("SELECT DISTINCT name FROM products WHERE is_approved = true AND LOWER(name) LIKE LOWER(?) LIMIT ?", String.class, "%" + keyword + "%", MAX_SUGGESTIONS);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    private String getSpellingSuggestion(String keyword) {
        try {
            return jdbcTemplate.queryForObject("SELECT name FROM products WHERE is_approved = true AND similarity(name, ?) > 0.3 ORDER BY similarity(name, ?) DESC LIMIT 1", String.class, keyword, keyword);
        } catch (Exception e) {
            return null;
        }
    }
}