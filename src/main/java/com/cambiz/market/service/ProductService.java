package com.cambiz.market.service;

import com.cambiz.market.dto.ProductRequest;
import com.cambiz.market.dto.ProductResponse;
import com.cambiz.market.model.Category;
import com.cambiz.market.model.Product;
import com.cambiz.market.model.User;
import com.cambiz.market.repository.CategoryRepository;
import com.cambiz.market.repository.ProductRepository;
import com.cambiz.market.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    // Create product (only for SELLERS)
    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        
        if (seller.getUserType() != User.UserType.SELLER) {
            throw new RuntimeException("Only sellers can add products");
        }
        
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountedPrice(request.getDiscountedPrice());
        product.setStockQuantity(request.getQuantity());
        product.setProductCondition(request.getCondition());
        product.setImageUrls(request.getImageUrls());
        product.setSeller(seller);
        product.setIsFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false);
        
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }
        
        product = productRepository.save(product);
        return ProductResponse.fromProduct(product);
    }
    
    // Get product by ID
    @Transactional
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
        product.getImageUrls().size();
        return ProductResponse.fromProduct(product);
    }
    
    // Get all products (public)
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findByIsActiveTrue(pageable);
        products.getContent().forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products.getContent());
    }
    
    // Get featured products
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts() {
        List<Product> products = productRepository.findTop10ByIsFeaturedTrueOrderByCreatedAtDesc();
        products.forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products);
    }
    
    // Get products by category
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
        products.getContent().forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products.getContent());
    }
    
    // Search products
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                keyword, keyword, pageable);
        products.getContent().forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products.getContent());
    }
    
    // Update product (only owner) - FIXED: Only update provided fields
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Long sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("You can only update your own products");
        }
        
        // ✅ Only update fields that are provided (not null)
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDiscountedPrice() != null) product.setDiscountedPrice(request.getDiscountedPrice());
        if (request.getQuantity() != null) product.setStockQuantity(request.getQuantity());
        if (request.getCondition() != null) product.setProductCondition(request.getCondition());
        if (request.getImageUrls() != null) product.setImageUrls(request.getImageUrls());
        if (request.getIsFeatured() != null) product.setIsFeatured(request.getIsFeatured());
        
        // ✅ Handle category assignment
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            product.setCategory(category);
        }
        
        product = productRepository.save(product);
        return ProductResponse.fromProduct(product);
    }
    
    // Delete product (only owner)
    @Transactional
    public void deleteProduct(Long id, Long sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("You can only delete your own products");
        }
        
        product.setIsActive(false);
        productRepository.save(product);
    }
    
    // Get seller's products
    @Transactional(readOnly = true)
    public List<ProductResponse> getSellerProducts(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        List<Product> products = productRepository.findBySeller(seller);
        products.forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products);
    }
}