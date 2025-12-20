package com.example.back_end.modules.catalog.category.controller;

import com.example.back_end.modules.catalog.category.dto.CategoryDTO;
import com.example.back_end.modules.catalog.category.dto.CategoryCreateDTO;
import com.example.back_end.modules.catalog.category.dto.CategoryUpdateDTO;
import com.example.back_end.modules.catalog.category.dto.CategoryTreeDTO;
import com.example.back_end.modules.catalog.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeDTO>> getTree() {
        return ResponseEntity.ok(service.getTree());
    }

    @GetMapping("/hierarchy")
    public ResponseEntity<List<CategoryDTO>> getCategoriesHierarchy() {
        return ResponseEntity.ok(service.getAllCategoriesHierarchy());
    }

    @GetMapping("/parents")
    public ResponseEntity<List<CategoryDTO>> getParentCategories() {
        return ResponseEntity.ok(service.getAllParentCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/{parentId}/sub-categories")
    public ResponseEntity<List<CategoryDTO>> getSubCategories(@PathVariable Long parentId) {
        return ResponseEntity.ok(service.getSubCategories(parentId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<CategoryDTO>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(service.getByProductId(productId));
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> create(@Valid @RequestBody CategoryCreateDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

