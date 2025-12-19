package com.example.back_end.modules.catalog.category.mapper;

import com.example.back_end.modules.catalog.category.dto.CategoryDTO;
import com.example.back_end.modules.catalog.category.dto.CategorySimpleDTO;
import com.example.back_end.modules.catalog.category.entity.Category;
import com.example.back_end.modules.catalog.product.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    private final ProductCategoryRepository productCategoryRepository;

    /**
     * Map Category to CategoryResponse
     */
    public CategoryDTO.CategoryResponse toCategoryResponse(Category category) {
        return CategoryDTO.CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .subCategories(category.getChildren().stream()
                        .map(this::toSubCategoryResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Map Category to SubCategoryResponse
     */
    public CategoryDTO.SubCategoryResponse toSubCategoryResponse(Category category) {
        Long productCount = productCategoryRepository.countProductsInCategory(category.getId());
        
        return CategoryDTO.SubCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .productCount(productCount.intValue())
                .build();
    }

    /**
     * Map Category to CategorySimple
     */
    public CategoryDTO.CategorySimple toCategorySimple(Category category) {
        return CategoryDTO.CategorySimple.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .build();
    }

    /**
     * Map Category to CategorySimpleDTO
     */
    public CategorySimpleDTO toCategorySimpleDTO(Category category) {
        return CategorySimpleDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}