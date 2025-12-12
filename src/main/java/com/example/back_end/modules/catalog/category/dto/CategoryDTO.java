package com.example.back_end.modules.catalog.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CategoryDTO {

    /**
     * Category response with sub-categories
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private String name;
        private Long parentId;
        private String parentName;
        private List<SubCategoryResponse> subCategories;
    }

    /**
     * Sub-category response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubCategoryResponse {
        private Long id;
        private String name;
        private Integer productCount;
    }

    /**
     * Simple category (for dropdowns)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySimple {
        private Long id;
        private String name;
        private Long parentId;
    }
}