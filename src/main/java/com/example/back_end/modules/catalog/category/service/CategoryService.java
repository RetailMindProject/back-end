package com.example.back_end.modules.catalog.category.service;

import com.example.back_end.exception.ResourceNotFoundException;
import com.example.back_end.modules.catalog.category.dto.CategoryDTO;
import com.example.back_end.modules.catalog.category.entity.Category;
import com.example.back_end.modules.catalog.category.mapper.CategoryMapper;
import com.example.back_end.modules.catalog.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Category operations
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Get all parent categories with their sub-categories
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO.CategoryResponse> getAllCategoriesHierarchy() {
        List<Category> parentCategories = categoryRepository.findAllParentCategories();
        return parentCategories.stream()
                .map(categoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get category by ID with sub-categories
     */
    @Transactional(readOnly = true)
    public CategoryDTO.CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        return categoryMapper.toCategoryResponse(category);
    }

    /**
     * Get all parent categories only
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO.CategorySimple> getAllParentCategories() {
        List<Category> categories = categoryRepository.findAllParentCategories();
        return categories.stream()
                .map(categoryMapper::toCategorySimple)
                .collect(Collectors.toList());
    }

    /**
     * Get sub-categories by parent ID
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO.SubCategoryResponse> getSubCategories(Long parentId) {
        // Verify parent exists
        categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentId));

        List<Category> subCategories = categoryRepository.findSubCategoriesByParentId(parentId);

        // نستخدم المابر + نحسب productCount الحقيقي من الريبو
        return subCategories.stream()
                .map(sub -> {
                    CategoryDTO.SubCategoryResponse dto = categoryMapper.toSubCategoryResponse(sub);
                    long count = categoryRepository.countProductsByCategoryId(sub.getId());
                    dto.setProductCount((int) count); // SubCategoryResponse يستخدم Integer
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all categories (flat list)
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO.CategorySimple> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(categoryMapper::toCategorySimple)
                .collect(Collectors.toList());
    }
}
