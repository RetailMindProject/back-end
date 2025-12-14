package com.example.back_end.modules.catalog.category.mapper;

import com.example.back_end.modules.catalog.category.dto.CategoryDTO;
import com.example.back_end.modules.catalog.category.entity.Category;

public final class CategoryMapper {

    private CategoryMapper() {}

    public static CategoryDTO toDto(Category entity) {
        if (entity == null) return null;
        return CategoryDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .build();
    }
}

