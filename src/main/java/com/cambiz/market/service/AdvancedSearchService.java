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
    
    public SearchResultDTO search(SearchCriteria criteria) {
        SearchResultDTO result = new SearchResultDTO();
        
        String keyword = criteria.getKeyword() != null ? criteria.getKeyword().trim() : "";
        
        // Simple search query without full-text (more stable)
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
        String sql = "SELECT p.id, p.name, COALESCE(p.description, ''), p.price, p.discounted_price, " +
                     "COALESCE(p.product_condition, ''), p.is_featured, p.stock_quantity, COALESCE(p.free_delivery, false), " +
                     "COALESCE(c.name, ''), COALESCE(c.emoji, ''), " +
                     "COALESCE(u.full_name, ''), COALESCE(u.city, ''), " +
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
                    .id(getLong(row, "id"))
                    .name(getString(row, "name"))
                    .description(getString(row, "description"))
                    .price(getDouble(row, "price"))
                    .discountedPrice(getDouble(row, "discounted_price"))
                    .productCondition(getString(row, "product_condition"))
                    .isFeatured(getBoolean(row, "is_featured"))
                    .inStock(getInt(row, "stock_quantity") > 0)
                    .freeDelivery(getBoolean(row, "free_delivery"))
                    .categoryName(getString(row, "name"))
                    .categoryEmoji(getString(row, "emoji"))
                    .sellerName(getString(row, "full_name"))
                    .sellerLocation(getString(row, "city"))
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
    
    private String getSpellingSuggestion(String keyword) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT name FROM products WHERE is_approved = true AND similarity(name, ?) > 0.3 ORDER BY similarity(name, ?) DESC LIMIT 1",
                String.class, keyword, keyword);
        } catch (Exception e) {
            return null;
        }
    }
}