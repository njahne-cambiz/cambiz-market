package com.cambiz.market.controller;

import com.cambiz.market.dto.ApiResponse;
import com.cambiz.market.model.Category;
import com.cambiz.market.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        // Convert to simple map to avoid circular reference
        List<Map<String, Object>> simplified = categories.stream().map(cat -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cat.getId());
            map.put("nameEn", cat.getNameEn());
            map.put("nameFr", cat.getNameFr());
            map.put("description", cat.getDescription());
            map.put("icon", cat.getIcon());
            map.put("sortOrder", cat.getSortOrder());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse(true, "Categories retrieved", simplified));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse(true, "Category retrieved", 
            categoryRepository.findById(id).orElse(null)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createCategory(@RequestBody Category category) {
        if (category.getNameEn() == null || category.getNameEn().isEmpty()) {
            category.setNameEn(category.getNameFr());
        }
        if (category.getNameFr() == null || category.getNameFr().isEmpty()) {
            category.setNameFr(category.getNameEn());
        }
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(new ApiResponse(true, "Category created", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        return ResponseEntity.ok(new ApiResponse(true, "Category updated", categoryRepository.save(category)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id) {
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse(true, "Category deleted", null));
    }
}