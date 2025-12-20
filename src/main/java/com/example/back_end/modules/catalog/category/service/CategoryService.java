package com.example.back_end.modules.catalog.category.service;

import com.example.back_end.modules.catalog.category.dto.CategoryDTO;
import com.example.back_end.modules.catalog.category.dto.CategoryCreateDTO;
import com.example.back_end.modules.catalog.category.dto.CategoryUpdateDTO;
import com.example.back_end.modules.catalog.category.dto.CategoryTreeDTO;

import java.util.List;

public interface CategoryService {
    List<CategoryDTO> getAll();
    List<CategoryDTO> getByProductId(Long productId);
    CategoryDTO getById(Long id);
    CategoryDTO create(CategoryCreateDTO dto);
    CategoryDTO update(Long id, CategoryUpdateDTO dto);
    void delete(Long id);
    List<CategoryTreeDTO> getTree();
    List<CategoryDTO> getAllCategoriesHierarchy();
    List<CategoryDTO> getAllParentCategories();
    List<CategoryDTO> getSubCategories(Long parentId);
}

