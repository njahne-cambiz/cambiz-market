package com.cambiz.market.service;

import com.cambiz.market.dto.ProductCardDTO;
import com.cambiz.market.dto.SearchCriteria;
import com.cambiz.market.dto.SearchResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdvancedSearchService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final int MAX_SUGGESTIONS = 5;
    
    public SearchResultDTO search(SearchCriteria criteria) {
        SearchResultDTO result = new SearchResultDTO();
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.id, p.name, p.description, p.price, p.discounted_price, ");
        sql.append("p.product_condition, p.is_featured, p.stock_quantity, p.free_delivery, ");
        sql.append("c.name as category_name, c.emoji as category_emoji, ");
        sql.append("u.full_name as seller_name, u.city as seller_location, ");
        sql.append("COALESCE(AVG(r.rating), 0) as avg_rating, ");
        sql.append("COUNT(DISTINCT r.id) as review_count ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN categories c ON p.category_id = c.id ");
        sql.append("LEFT JOIN users u ON p.seller_id = u.id ");
        sql.append("LEFT JOIN reviews r ON p.id = r.product_id ");
        sql.append("WHERE p.is_approved = true AND p.is_active = true ");
        
        Map<String, Object> params = new HashMap<>();
        
        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            String searchTerm = criteria.getKeyword().trim();
            sql.append("AND (p.search_vector @@ plainto_tsquery('english', :searchTerm) ");
            params.put("searchTerm", searchTerm);
            sql.append("OR LOWER(p.name) LIKE LOWER(:keywordLike)) ");
            params.put("keywordLike", "%" + searchTerm + "%");
            result.setDidYouMean(getSpellingSuggestion(searchTerm));
        }
        
        if (criteria.getCategoryId() != null) {
            sql.append("AND p.category_id = :categoryId ");
            params.put("categoryId", criteria.getCategoryId());
        }
        
        if (criteria.getMinPrice() != null) {
            sql.append("AND COALESCE(p.discounted_price, p.price) >= :minPrice ");
            params.put("minPrice", criteria.getMinPrice());
        }
        if (criteria.getMaxPrice() != null) {
            sql.append("AND COALESCE(p.discounted_price, p.price) <= :maxPrice ");
            params.put("maxPrice", criteria.getMaxPrice());
        }
        
        if (criteria.getLocation() != null && !criteria.getLocation().isEmpty()) {
            sql.append("AND LOWER(u.city) LIKE LOWER(:location) ");
            params.put("location", "%" + criteria.getLocation() + "%");
        }
        
        if (criteria.getProductCondition() != null && !criteria.getProductCondition().isEmpty()) {
            sql.append("AND p.product_condition = :condition ");
            params.put("condition", criteria.getProductCondition());
        }
        
        if (criteria.getInStock() != null && criteria.getInStock()) {
            sql.append("AND p.stock_quantity > 0 ");
        }
        
        if (criteria.getIsFeatured() != null && criteria.getIsFeatured()) {
            sql.append("AND p.is_featured = true ");
        }
        
        if (criteria.getOnDiscount() != null && criteria.getOnDiscount()) {
            sql.append("AND p.discounted_price IS NOT NULL ");
        }
        
        if (criteria.getFreeDelivery() != null && criteria.getFreeDelivery()) {
            sql.append("AND p.free_delivery = true ");
        }
        
        sql.append("GROUP BY p.id, c.name, c.emoji, u.full_name, u.city ");
        
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "relevance";
        switch (sortBy) {
            case "price_asc":
                sql.append("ORDER BY COALESCE(p.discounted_price, p.price) ASC ");
                break;
            case "price_desc":
                sql.append("ORDER BY COALESCE(p.discounted_price, p.price) DESC ");
                break;
            case "rating":
                sql.append("ORDER BY avg_rating DESC ");
                break;
            case "newest":
                sql.append("ORDER BY p.created_at DESC ");
                break;
            case "popular":
                sql.append("ORDER BY p.view_count DESC ");
                break;
            default:
                if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
                    sql.append("ORDER BY ts_rank(p.search_vector, plainto_tsquery('english', :rankTerm)) DESC, ");
                    params.put("rankTerm", criteria.getKeyword().trim());
                }
                sql.append("p.is_featured DESC, p.created_at DESC ");
                break;
        }
        
        String countSql = "SELECT COUNT(*) FROM (" + sql.toString() + ") as subquery";
        Query countQuery = entityManager.createNativeQuery(countSql);
        params.forEach(countQuery::setParameter);
        long totalResults = ((Number) countQuery.getSingleResult()).longValue();
        
        int page = criteria.getPage();
        int size = criteria.getSize();
        sql.append("LIMIT :limit OFFSET :offset ");
        params.put("limit", size);
        params.put("offset", page * size);
        
        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<ProductCardDTO> products = results.stream()
            .map(this::mapToProductCardDTO)
            .collect(Collectors.toList());
        
        result.setProducts(products);
        result.setTotalResults(totalResults);
        result.setTotalPages((int) Math.ceil((double) totalResults / size));
        result.setCurrentPage(page);
        result.setCategoryFacets(getCategoryFacets());
        result.setLocationFacets(getLocationFacets());
        result.setPriceRangeFacets(getPriceRangeFacets());
        result.setConditionFacets(getConditionFacets());
        
        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            result.setSuggestedKeywords(getSearchSuggestions(criteria.getKeyword()));
        }
        
        return result;
    }
    
    private ProductCardDTO mapToProductCardDTO(Object[] row) {
        String imageUrl = null;
        try {
            if (row.length > 15 && row[15] != null) {
                imageUrl = extractFirstImage(row[15]);
            }
        } catch (Exception e) {
            imageUrl = null;
        }
        
        return ProductCardDTO.builder()
            .id(row[0] != null ? ((Number) row[0]).longValue() : null)
            .name((String) row[1])
            .description((String) row[2])
            .price((Double) row[3])
            .discountedPrice((Double) row[4])
            .productCondition((String) row[5])
            .isFeatured((Boolean) row[6])
            .inStock(row[7] != null && ((Integer) row[7]) > 0)
            .freeDelivery((Boolean) row[8])
            .categoryName((String) row[9])
            .categoryEmoji((String) row[10])
            .sellerName((String) row[11])
            .sellerLocation((String) row[12])
            .rating(row[13] != null ? (Double) row[13] : 0.0)
            .reviewCount(row[14] != null ? ((Number) row[14]).intValue() : 0)
            .imageUrl(imageUrl)
            .build();
    }
    
    private String extractFirstImage(Object imageUrls) {
        if (imageUrls == null) return null;
        String urls = imageUrls.toString();
        if (urls.startsWith("{") && urls.endsWith("}")) {
            String[] parts = urls.substring(1, urls.length() - 1).split(",");
            if (parts.length > 0) {
                return parts[0].replace("\"", "").trim();
            }
        }
        return urls;
    }
    
    private Map<String, Long> getCategoryFacets() {
        String sql = "SELECT c.name, COUNT(p.id) FROM products p JOIN categories c ON p.category_id = c.id WHERE p.is_approved = true AND p.is_active = true GROUP BY c.name ORDER BY COUNT(p.id) DESC LIMIT 10";
        
        Map<String, Long> facets = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            try {
                facets.put(rs.getString(1), rs.getLong(2));
            } catch (Exception e) {
                // skip
            }
        });
        return facets;
    }
    
    private Map<String, Long> getLocationFacets() {
        String sql = "SELECT u.city, COUNT(p.id) FROM products p JOIN users u ON p.seller_id = u.id WHERE p.is_approved = true AND u.city IS NOT NULL GROUP BY u.city ORDER BY COUNT(p.id) DESC LIMIT 10";
        
        Map<String, Long> facets = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            try {
                facets.put(rs.getString(1), rs.getLong(2));
            } catch (Exception e) {
                // skip
            }
        });
        return facets;
    }
    
    private Map<String, Long> getPriceRangeFacets() {
        Map<String, Long> facets = new LinkedHashMap<>();
        int[][] ranges = {{0, 5000}, {5001, 10000}, {10001, 25000}, {25001, 50000}, {50001, 100000}, {100001, Integer.MAX_VALUE}};
        String[] labels = {"Under 5k", "5k-10k", "10k-25k", "25k-50k", "50k-100k", "100k+"};
        
        for (int i = 0; i < ranges.length; i++) {
            try {
                Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM products WHERE is_approved=true AND is_active=true AND COALESCE(discounted_price, price) BETWEEN ? AND ?",
                    Long.class, ranges[i][0], ranges[i][1]);
                if (count != null && count > 0) {
                    facets.put(labels[i], count);
                }
            } catch (Exception e) {
                // skip
            }
        }
        return facets;
    }
    
    private Map<String, Long> getConditionFacets() {
        String sql = "SELECT product_condition, COUNT(id) FROM products WHERE is_approved = true AND is_active = true AND product_condition IS NOT NULL GROUP BY product_condition";
        
        Map<String, Long> facets = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            try {
                facets.put(rs.getString(1), rs.getLong(2));
            } catch (Exception e) {
                // skip
            }
        });
        return facets;
    }
    
    private List<String> getSearchSuggestions(String keyword) {
        try {
            return jdbcTemplate.queryForList(
                "SELECT DISTINCT name FROM products WHERE is_approved = true AND (LOWER(name) LIKE LOWER(?) OR search_vector @@ plainto_tsquery('english', ?)) LIMIT ?",
                String.class, "%" + keyword + "%", keyword, MAX_SUGGESTIONS);
        } catch (Exception e) {
            return Collections.emptyList();
        }
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