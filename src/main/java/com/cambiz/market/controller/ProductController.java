package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.dto.ProductRequest;
import com.cambiz.market.dto.ProductResponse;
import com.cambiz.market.service.ProductService;
import com.cambiz.market.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private UserService userService;

    // CREATE PRODUCT (SELLER only)
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        try {
            Long sellerId = getCurrentUserId();
            ProductResponse product = productService.createProduct(request, sellerId);
            return ResponseEntity.ok(new ApiResponse(true, "Product created successfully!", product));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // GET ALL PRODUCTS (Public)
    @GetMapping
    public ResponseEntity<?> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<ProductResponse> products = productService.getAllProducts(page, size);
            return ResponseEntity.ok(new ApiResponse(true, "Products retrieved", products));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // GET FEATURED PRODUCTS (Public)
    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedProducts() {
        try {
            List<ProductResponse> products = productService.getFeaturedProducts();
            return ResponseEntity.ok(new ApiResponse(true, "Featured products retrieved", products));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // GET PRODUCT BY ID (Public)
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        try {
            ProductResponse product = productService.getProduct(id);
            return ResponseEntity.ok(new ApiResponse(true, "Product retrieved", product));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // GET PRODUCTS BY CATEGORY (Public)
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getProductsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<ProductResponse> products = productService.getProductsByCategory(categoryId, page, size);
            return ResponseEntity.ok(new ApiResponse(true, "Products retrieved", products));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // SEARCH PRODUCTS (Public)
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<ProductResponse> products = productService.searchProducts(keyword, page, size);
            return ResponseEntity.ok(new ApiResponse(true, "Search results", products));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // UPDATE PRODUCT (SELLER only)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request) {
        try {
            Long sellerId = getCurrentUserId();
            ProductResponse product = productService.updateProduct(id, request, sellerId);
            return ResponseEntity.ok(new ApiResponse(true, "Product updated successfully!", product));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // DELETE PRODUCT (SELLER only)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            Long sellerId = getCurrentUserId();
            productService.deleteProduct(id, sellerId);
            return ResponseEntity.ok(new ApiResponse(true, "Product deleted successfully!", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // GET MY PRODUCTS (SELLER)
    @GetMapping("/my-products")
    public ResponseEntity<?> getMyProducts() {
        try {
            Long sellerId = getCurrentUserId();
            List<ProductResponse> products = productService.getSellerProducts(sellerId);
            return ResponseEntity.ok(new ApiResponse(true, "Your products", products));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage(), null));
        }
    }

    // Helper method to get current user ID from SecurityContext
    private Long getCurrentUserId() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String email = userDetails.getUsername();
        return userService.getUserIdByEmail(email);
    }
}