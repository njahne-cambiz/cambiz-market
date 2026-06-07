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
        
        StringBuilder whereClause = new StringBuilder("WHERE p.is_approved = true AND p.is_active = true ");
        List<Object> params = new ArrayList<>();
        
        if (!keyword.isEmpty()) {
            whereClause.append("AND LOWER(p.name) LIKE LOWER(?) ");
            params.add("%" + keyword + "%");
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
        
        if (criteria.getInStock() != null && criteria.getInStock()) {
            whereClause.append("AND p.stock_quantity > 0 ");
        }
        
        if (criteria.getFreeDelivery() != null && criteria.getFreeDelivery()) {
            whereClause.append("AND p.free_delivery = true ");
        }
        
        // Count
        String countSql = "SELECT COUNT(*) FROM products p " + whereClause.toString();
        long totalResults = 0;
        try {
            Long count = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
            totalResults = count != null ? count : 0;
        } catch (Exception e) {
            totalResults = 0;
        }
        
        // Sort
        String orderBy = "ORDER BY p.is_featured DESC, p.created_at DESC";
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "relevance";
        if ("price_asc".equals(sortBy)) orderBy = "ORDER BY COALESCE(p.discounted_price, p.price) ASC";
        else if ("price_desc".equals(sortBy)) orderBy = "ORDER BY COALESCE(p.discounted_price, p.price) DESC";
        
        int page = criteria.getPage();
        int size = criteria.getSize();
        
        // Main query - NO GROUP BY
        String sql = "SELECT p.id, p.name, COALESCE(p.description,'') as description, p.price, " +
                     "COALESCE(p.discounted_price,0) as discounted_price, " +
                     "COALESCE(p.product_condition,'') as product_condition, " +
                     "p.is_featured, p.stock_quantity, COALESCE(p.free_delivery,false) as free_delivery, " +
                     "COALESCE(c.name,'') as cat_name, COALESCE(c.emoji,'') as cat_emoji, " +
                     "COALESCE(u.full_name,'') as seller_name, COALESCE(u.city,'') as seller_city " +
                     "FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "LEFT JOIN users u ON p.seller_id = u.id " +
                     whereClause.toString() +
                     " " + orderBy + " LIMIT ? OFFSET ?";
        
        params.add(size);
        params.add(page * size);
        
        List<ProductCardDTO> products = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());
            for (Map<String, Object> row : rows) {
                ProductCardDTO dto = ProductCardDTO.builder()
                    .id(toLong(row.get("id")))
                    .name(toString(row.get("name")))
                    .description(toString(row.get("description")))
                    .price(toDouble(row.get("price")))
                    .discountedPrice(toDouble(row.get("discounted_price")))
                    .productCondition(toString(row.get("product_condition")))
                    .isFeatured(toBoolean(row.get("is_featured")))
                    .inStock(toInt(row.get("stock_quantity")) > 0)
                    .freeDelivery(toBoolean(row.get("free_delivery")))
                    .categoryName(toString(row.get("cat_name")))
                    .categoryEmoji(toString(row.get("cat_emoji")))
                    .sellerName(toString(row.get("seller_name")))
                    .sellerLocation(toString(row.get("seller_city")))
                    .rating(0.0)
                    .reviewCount(0)
                    .imageUrl(null)
                    .build();
                products.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        result.setProducts(products);
        result.setTotalResults(totalResults);
        result.setTotalPages(totalResults > 0 ? (int) Math.ceil((double) totalResults / size) : 0);
        result.setCurrentPage(page);
        
        return result;
    }
    
    private String toString(Object val) {
        return val != null ? val.toString() : "";
    }
    
    private Long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }
    
    private Double toDouble(Object val) {
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }
    
    private Integer toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }
    
    private Boolean toBoolean(Object val) {
        if (val instanceof Boolean) return (Boolean) val;
        return false;
    }
}