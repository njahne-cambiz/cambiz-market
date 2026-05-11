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

import java.time.LocalDateTime;
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
        
        // Set approval status - new products need admin approval
        product.setIsApproved(false);
        product.setStatus(Product.ProductStatus.PENDING_APPROVAL);
        
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
    
    // Get all products for public store - ONLY APPROVED & ACTIVE
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findActiveAndApproved(pageable);
        products.getContent().forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products.getContent());
    }
    
    // Get featured products - ONLY APPROVED
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts() {
        List<Product> products = productRepository.findTop10ByIsFeaturedTrueAndApprovedOrderByCreatedAtDesc();
        products.forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products);
    }
    
    // Get products by category - ONLY APPROVED
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findByCategoryIdAndActiveAndApproved(categoryId, pageable);
        products.getContent().forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products.getContent());
    }
    
    // Search products - ONLY APPROVED
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.searchApprovedProducts(keyword, pageable);
        products.getContent().forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products.getContent());
    }
    
    // Update product (only owner)
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Long sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        if (!product.getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("You can only update your own products");
        }
        
        // Only update fields that are provided (not null)
        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDiscountedPrice() != null) product.setDiscountedPrice(request.getDiscountedPrice());
        if (request.getQuantity() != null) product.setStockQuantity(request.getQuantity());
        if (request.getCondition() != null) product.setProductCondition(request.getCondition());
        if (request.getImageUrls() != null) product.setImageUrls(request.getImageUrls());
        if (request.getIsFeatured() != null) product.setIsFeatured(request.getIsFeatured());
        
        // Reset to pending if product is updated (admin should re-review)
        product.setIsApproved(false);
        product.setStatus(Product.ProductStatus.PENDING_APPROVAL);
        product.setRejectionReason(null);
        
        // Handle category assignment
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
    
    // Get seller's products (all statuses for seller dashboard)
    @Transactional(readOnly = true)
    public List<ProductResponse> getSellerProducts(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        List<Product> products = productRepository.findBySeller(seller);
        products.forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products);
    }
    
    // Get Product entity by ID (not DTO) — used for image upload operations
    @Transactional(readOnly = true)
    public Product getProductEntity(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        product.getImageUrls().size(); // Initialize lazy collection
        return product;
    }
    
    // Save/update a Product entity directly — used for image upload/deletion
    @Transactional
    public void updateProductEntity(Product product) {
        productRepository.save(product);
    }
    
    // ========== PRODUCT APPROVAL METHODS ==========
    
    // Get pending approval products (for admin)
    @Transactional(readOnly = true)
    public List<ProductResponse> getPendingApprovalProducts() {
        List<Product> products = productRepository.findByIsApprovedFalseOrIsApprovedNull();
        products.forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products);
    }
    
    // Get approved products (for admin)
    @Transactional(readOnly = true)
    public List<ProductResponse> getApprovedProducts() {
        List<Product> products = productRepository.findByIsApprovedTrue();
        products.forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products);
    }
    
    // Get rejected products (for admin)
    @Transactional(readOnly = true)
    public List<ProductResponse> getRejectedProducts() {
        List<Product> products = productRepository.findByIsApprovedFalseAndRejectionReasonNotNull();
        products.forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products);
    }
    
    // Get all products for admin (including pending/rejected)
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProductsAdmin() {
        List<Product> products = productRepository.findAll();
        products.forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(products);
    }
    
    // Admin delete product
    @Transactional
    public void adminDeleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setIsActive(false);
        productRepository.save(product);
    }
    
    // Approve a product
    @Transactional
    public ProductResponse approveProduct(Long productId, String adminEmail) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        product.setIsApproved(true);
        product.setIsActive(true);
        product.setStatus(Product.ProductStatus.APPROVED);
        product.setApprovedBy(adminEmail);
        product.setApprovedAt(LocalDateTime.now());
        product.setRejectionReason(null); // Clear any previous rejection
        
        product = productRepository.save(product);
        return ProductResponse.fromProduct(product);
    }
    
    // Reject a product
    @Transactional
    public ProductResponse rejectProduct(Long productId, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        product.setIsApproved(false);
        product.setIsActive(false);
        product.setStatus(Product.ProductStatus.REJECTED);
        product.setRejectionReason(reason);
        product.setRejectedAt(LocalDateTime.now());
        
        product = productRepository.save(product);
        return ProductResponse.fromProduct(product);
    }
    
    // Batch approve products
    @Transactional
    public int batchApproveProducts(List<Long> productIds, String adminEmail) {
        int approvedCount = 0;
        for (Long id : productIds) {
            Product product = productRepository.findById(id).orElse(null);
            if (product != null && (product.getIsApproved() == null || !product.getIsApproved())) {
                product.setIsApproved(true);
                product.setIsActive(true);
                product.setStatus(Product.ProductStatus.APPROVED);
                product.setApprovedBy(adminEmail);
                product.setApprovedAt(LocalDateTime.now());
                product.setRejectionReason(null);
                productRepository.save(product);
                approvedCount++;
            }
        }
        return approvedCount;
    }
    
    // Get pending approval count (for admin badge)
    @Transactional(readOnly = true)
    public long getPendingApprovalCount() {
        return productRepository.countByIsApprovedFalseOrIsApprovedNull();
    }
    
    // Get total products count
    @Transactional(readOnly = true)
    public long getTotalProducts() {
        return productRepository.count();
    }
    
    // Get top selling products (for analytics)
    @Transactional(readOnly = true)
    public List<ProductResponse> getTopSellingProducts() {
        List<Product> products = productRepository.findAll();
        products.sort((a, b) -> b.getViewCount().compareTo(a.getViewCount()));
        List<Product> top10 = products.size() > 10 ? products.subList(0, 10) : products;
        top10.forEach(p -> p.getImageUrls().size());
        return ProductResponse.fromProducts(top10);
    }
}