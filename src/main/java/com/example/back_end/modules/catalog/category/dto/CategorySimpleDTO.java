package com.example.back_end.modules.catalog.category.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySimpleDTO {
    private Long id;
    private String name;
}