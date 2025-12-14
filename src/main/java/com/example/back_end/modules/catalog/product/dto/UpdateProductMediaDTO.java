package com.example.back_end.modules.catalog.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProductMediaDTO {
    private String url;
    private String mimeType;
    private String title;
    private String altText;
    private Integer sortOrder;
    private Boolean isPrimary;
}

