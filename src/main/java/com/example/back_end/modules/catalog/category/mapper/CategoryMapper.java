package com.example.back_end.modules.catalog.category.mapper;

import com.example.back_end.modules.catalog.category.dto.CategoryDTO;
import com.example.back_end.modules.catalog.category.dto.CategoryTreeDTO;
import com.example.back_end.modules.catalog.category.entity.Category;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    public CategoryDTO toDto(Category entity) {
        if (entity == null) return null;
        return CategoryDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .parentName(entity.getParent() != null ? entity.getParent().getName() : null)
                .build();
    }

    public CategoryDTO toDtoWithChildren(Category entity) {
        if (entity == null) return null;
        CategoryDTO dto = toDto(entity);
        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            dto.setSubCategories(entity.getChildren().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public CategoryDTO toDtoWithProductCount(Category entity, Integer productCount) {
        if (entity == null) return null;
        CategoryDTO dto = toDto(entity);
        dto.setProductCount(productCount);
        return dto;
    }

    public CategoryTreeDTO toTreeDto(Category entity) {
        if (entity == null) return null;
        CategoryTreeDTO dto = CategoryTreeDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .build();

        if (entity.getChildren() != null && !entity.getChildren().isEmpty()) {
            dto.setChildren(entity.getChildren().stream()
                    .map(this::toTreeDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}

