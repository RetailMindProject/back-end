package com.example.back_end.modules.catalog.category.controller;

import com.example.back_end.modules.catalog.category.entity.Category;
import com.example.back_end.modules.catalog.category.repository.CategoryRepository;
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

    /**
     * Get all categories
     * GET /api/categories
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        try {
            List<Category> categories = categoryRepository.findAll();

            // Map to simple DTO for dropdown
            List<Map<String, Object>> categoryList = categories.stream()
                    .map(category -> {
                        Map<String, Object> cat = new HashMap<>();
                        cat.put("id", category.getId());
                        cat.put("name", category.getName());
                        return cat;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categoryList);
            response.put("count", categoryList.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching categories: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get category by ID
     * GET /api/categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCategoryById(@PathVariable Long id) {
        try {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));

            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("id", category.getId());
            categoryData.put("name", category.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categoryData);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching category: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}