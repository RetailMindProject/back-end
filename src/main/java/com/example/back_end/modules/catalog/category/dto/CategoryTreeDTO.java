package com.example.back_end.modules.catalog.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryTreeDTO {
    private Long id;
    private String name;
    private Long parentId;
    @Builder.Default
    private List<CategoryTreeDTO> children = new ArrayList<>();
}

